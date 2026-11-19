package net.spectre.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.world.level.block.state.BlockState;

/** Isolated submissions for offscreen icons; never drains vanilla's hand/GUI queue. */
public final class RenderBridge {
    private static SubmitNodeStorage icons=new SubmitNodeStorage();
    private RenderBridge(){}
    public static SubmitNodeStorage icons(){return icons;}
    public static void clearIcons(){icons=new SubmitNodeStorage();}
    public static void flushIcons(){
        try{net.spectre.client.util.FeatureCompat.renderAll(icons);}
        finally{clearIcons();}
    }
    public static boolean translucent(BlockState state){
        // 1.21.11: translucency is decided by the block's chunk render layer.
        return net.minecraft.client.renderer.ItemBlockRenderTypes.getChunkRenderType(state)==net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT;
    }
    public static int tint(BlockState state){
        return Minecraft.getInstance().getBlockColors().getColor(state,null,null,0);
    }
}
