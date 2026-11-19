package net.spectre.client.gui.ui;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Unified, frame-rate independent motion: damped springs (tactile, carry velocity), timed tweens
 * and a keyed store that evicts animations nobody has touched for a while.
 * <p>
 * {@link #beginFrame(float, float)} is called once per ImGui frame. Animation speed 0 means reduced
 * motion: every spring snaps to its target.
 */
@Environment(EnvType.CLIENT)
public final class Anim {
   /** Fast and critically damped: hover, focus, colour fades. */
   public static final Preset SNAPPY = new Preset(520.0F, 1.0F);
   /** Under-damped with a small overshoot: presses, toggles, indicators. */
   public static final Preset TACTILE = new Preset(420.0F, 0.62F);
   /** Soft and slower: panels, page transitions, layout. */
   public static final Preset GENTLE = new Preset(170.0F, 0.9F);
   /** Very stiff, nearly critical: dragged objects that must track the cursor closely. */
   public static final Preset DRAG = new Preset(1100.0F, 0.9F);
   private static final float MAX_STEP = 0.004F;
   private static final long EVICT_NANOS = 2_000_000_000L;
   private static final Long2ObjectOpenHashMap<Spring> SPRINGS = new Long2ObjectOpenHashMap<>();
   private static float dt;
   private static float speed = 1.0F;
   private static long frameNanos = System.nanoTime();
   private static final long START = frameNanos;
   private static int frame;

   private Anim() {
   }

   /** Advance the shared clock. {@code rawDt} is the clamped frame delta in seconds. */
   public static void beginFrame(float rawDt, float animationSpeed) {
      speed = animationSpeed;
      dt = Math.max(0.0F, Math.min(0.1F, rawDt)) * Math.max(0.0F, animationSpeed);
      frameNanos = System.nanoTime();
      if ((++frame & 63) == 0) {
         evict();
      }
   }

   public static float dt() {
      return dt;
   }

   public static boolean reducedMotion() {
      return speed <= 0.0F;
   }

   /** Seconds since start, for ambient effects (fog drift, flicker, wisps). */
   public static double time() {
      return (frameNanos - START) * 1.0E-9;
   }

   public static long nanos() {
      return frameNanos;
   }

   // ---- keyed springs ----------------------------------------------------------------------
   /** Spring value for {@code key} moving towards {@code target}. Creates the spring on first use. */
   public static float spring(long key, float target, Preset preset) {
      Spring s = SPRINGS.get(key);
      if (s == null) {
         s = new Spring(target);
         SPRINGS.put(key, s);
      }

      s.touched = frameNanos;
      return s.update(target, preset);
   }

   /** Same as {@link #spring} but starting from {@code from} when the spring is new (entrances). */
   public static float spring(long key, float target, float from, Preset preset) {
      Spring s = SPRINGS.get(key);
      if (s == null) {
         s = new Spring(from);
         SPRINGS.put(key, s);
      }

      s.touched = frameNanos;
      return s.update(target, preset);
   }

   /** Gives a spring an instantaneous kick (press feedback). */
   public static void kick(long key, float velocity) {
      Spring s = SPRINGS.get(key);
      if (s != null && !reducedMotion()) {
         s.velocity += velocity;
      }
   }

   public static float velocity(long key) {
      Spring s = SPRINGS.get(key);
      return s == null ? 0.0F : s.velocity;
   }

   public static long key(String ns, int id) {
      return (long)ns.hashCode() << 32 ^ id & 0xFFFFFFFFL;
   }

   public static long key(int ns, int id) {
      return (long)ns << 32 ^ id & 0xFFFFFFFFL;
   }

   private static void evict() {
      ObjectIterator<Long2ObjectMap.Entry<Spring>> it = SPRINGS.long2ObjectEntrySet().fastIterator();

      while (it.hasNext()) {
         if (frameNanos - it.next().getValue().touched > EVICT_NANOS) {
            it.remove();
         }
      }
   }

   // ---- easing helpers -------------------------------------------------------------------
   public static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
   }

   public static float easeOutCubic(float t) {
      t = clamp01(t);
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   public static float easeInOutCubic(float t) {
      t = clamp01(t);
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
   }

   /** Exponential approach that is independent of frame rate. */
   public static float approach(float from, float to, float rate) {
      if (reducedMotion()) {
         return to;
      } else {
         float v = from + (to - from) * (1.0F - (float)Math.exp(-rate * dt));
         return Math.abs(v - to) < 1.0E-4F ? to : v;
      }
   }

   /** Progress 0..1 of a timed transition that started at {@code startNanos}. */
   public static float progress(long startNanos, float durationMs) {
      if (reducedMotion() || durationMs <= 0.0F) {
         return 1.0F;
      } else {
         return clamp01((frameNanos - startNanos) / 1.0E6F / (durationMs / Math.max(0.05F, speed)));
      }
   }

   /** Staggered entrance progress for item {@code index}: per-item delay, capped total delay. */
   public static float stagger(long startNanos, int index, float perItemMs, float maxDelayMs, float durationMs) {
      float delay = Math.min(maxDelayMs, index * perItemMs);
      if (reducedMotion()) {
         return 1.0F;
      } else {
         float elapsed = (frameNanos - startNanos) / 1.0E6F * Math.max(0.05F, speed) - delay;
         return easeOutCubic(elapsed / durationMs);
      }
   }

   public record Preset(float stiffness, float dampingRatio) {
      float damping() {
         return 2.0F * this.dampingRatio * (float)Math.sqrt(this.stiffness);
      }
   }

   /** A unit-mass damped spring integrated with sub-stepped semi-implicit Euler. */
   public static final class Spring {
      private float value;
      private float velocity;
      long touched;

      public Spring(float value) {
         this.value = value;
      }

      public float update(float target, Preset preset) {
         if (Anim.reducedMotion()) {
            this.value = target;
            this.velocity = 0.0F;
            return target;
         } else {
            float remaining = Anim.dt;
            float k = preset.stiffness();
            float c = preset.damping();

            while (remaining > 0.0F) {
               float h = Math.min(MAX_STEP, remaining);
               float accel = k * (target - this.value) - c * this.velocity;
               this.velocity += accel * h;
               this.value += this.velocity * h;
               remaining -= h;
            }

            if (Math.abs(target - this.value) < 1.0E-4F && Math.abs(this.velocity) < 1.0E-3F) {
               this.value = target;
               this.velocity = 0.0F;
            }

            return this.value;
         }
      }

      public float value() {
         return this.value;
      }

      public void set(float value) {
         this.value = value;
         this.velocity = 0.0F;
      }

      public void kick(float v) {
         this.velocity += v;
      }
   }
}
