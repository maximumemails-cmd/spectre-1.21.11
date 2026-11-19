package net.spectre.client.mixinplugin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Skips Spectre mixins whose targets another installed mod replaces outright.
 * <p>
 * Sodium {@code @Overwrite}s {@code LevelRenderer.cullTerrain} and {@code isSectionCompiledAndVisible};
 * Mixin refuses to inject into methods merged by another mixin, which would abort startup.
 */
public final class SpectreMixinPlugin implements IMixinConfigPlugin {
   private boolean sodium;

   @Override
   public void onLoad(String mixinPackage) {
      this.sodium = FabricLoader.getInstance().isModLoaded("sodium");
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (mixinClassName.endsWith(".MixinVanillaCullingLevelRenderer")) {
         return !this.sodium;
      }
      return true;
   }

   @Override
   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   @Override
   public List<String> getMixins() {
      return null;
   }

   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
