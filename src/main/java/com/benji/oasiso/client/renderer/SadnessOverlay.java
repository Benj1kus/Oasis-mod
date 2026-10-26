package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.effect.SadnessStun;
import net.minecraft.world.entity.Mob;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.*;

@Mod.EventBusSubscriber(modid=Oasiso.MODID, value=Dist.CLIENT)
public final class SadnessOverlay {
    private static boolean drawing;
    private static final int[] BAYER={0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5};
    private record Mesh(List<Vec3[]> quads, double footY, double height, double fade) {}
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (drawing || event.getStage()!=RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        Minecraft mc=Minecraft.getInstance();
        if (mc.level==null) return;
        Vec3 eye=event.getCamera().getPosition();
        float partial=event.getPartialTick();
        var mobs=new ArrayList<Mob>();
        for (var e:mc.level.entitiesForRendering()) if (e instanceof Mob mob && SadnessStun.stunned(mob)
                && !mob.isInvisible() && mob.distanceToSqr(eye)<48*48) mobs.add(mob);
        mobs.sort(Comparator.comparingDouble((Mob e)->e.distanceToSqr(eye)));
        if (mobs.isEmpty()) return;
        var meshes=new ArrayList<Mesh>();
        drawing=true;
        try {
            for (int i=0;i<Math.min(16,mobs.size());i++) {
                Mob mob=mobs.get(i);
                double x=Mth.lerp(partial,mob.xOld,mob.getX())-eye.x;
                double y=Mth.lerp(partial,mob.yOld,mob.getY())-eye.y;
                double z=Mth.lerp(partial,mob.zOld,mob.getZ())-eye.z;
                PoseStack poses=new PoseStack(); poses.translate(x,y,z);
                Capture capture=new Capture();
                capture(mc,mob,partial,poses,capture);
                double fade=Math.min(1,mob.getEffect(SadnessStun.INSTANCE).getDuration()/10.0);
                meshes.add(new Mesh(capture.quads,y,Math.max(.2,mob.getBbHeight()),fade));
            }
        } finally { drawing=false; }
        if (meshes.isEmpty()) return;
        var shader = RenderSystem.getShader(); float[] color = RenderSystem.getShaderColor().clone();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int sr=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dr=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                sa=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), da=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        boolean offset=GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        float factor=GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR), units=GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
        try {
            RenderSystem.enablePolygonOffset(); RenderSystem.polygonOffset(-1,-1);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc(); RenderSystem.disableCull(); RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder out = Tesselator.getInstance().getBuilder();
            out.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            // The same AFTER_WEATHER / camera-relative convention as the working fountain renderer.
            Matrix4f matrix = new Matrix4f();
            double time=mc.level.getGameTime()+partial;
            for (int i=meshes.size()-1;i>=0;i--) {
                Mesh mesh=meshes.get(i);
                for (Vec3[] q:mesh.quads) drawQuad(out,matrix,q,mesh,time);
            }
            BufferUploader.drawWithShader(out.end());
        } finally {
            RenderSystem.polygonOffset(factor,units); if(offset)RenderSystem.enablePolygonOffset();else RenderSystem.disablePolygonOffset();
            RenderSystem.setShader(() -> shader); RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.depthMask(write); if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(sr,dr,sa,da); if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void capture(Minecraft mc,Mob mob,float partial,PoseStack poses,Capture capture) {
        EntityRenderer renderer=mc.getEntityRenderDispatcher().getRenderer(mob);
        renderer.render(mob,Mth.rotLerp(partial,mob.yRotO,mob.getYRot()),partial,poses,capture,0xF000F0);
    }
    private static void drawQuad(BufferBuilder out,Matrix4f matrix,Vec3[] q,Mesh mesh,double time) {
        for(int row=0;row<4;row++) for(int col=0;col<4;col++) {
            double u0=col/4.0,u1=(col+1)/4.0,v0=row/4.0,v1=(row+1)/4.0;
            Vec3 a=point(q,u0,v0),b=point(q,u1,v0),c=point(q,u1,v1),d=point(q,u0,v1);
            Vec3 center=a.add(c).scale(.5);
            double h=(center.y-mesh.footY)/mesh.height;
            // Stair-stepped foam edge moving from the head down towards the feet.
            double wobble=Math.round((Math.sin(center.x*8+time*.10)+Math.cos(center.z*7-time*.08))*3)/36.0;
            double front=1.15-(time%36)/36.0*1.4+wobble;
            double delta=h-front;
            boolean foam=Math.abs(delta)<.045;
            boolean water=delta>=.045 && delta<.25;
            double coverage=mesh.fade*(foam?.92:water?.85:.75);
            if(coverage <= (BAYER[col+4*row]+.5)/16.0) continue;
            int r=foam?195:water?65:65, g=foam?243:water?175:155, bl=foam?255:water?235:225;
            int alpha=(int)((foam?155:water?90:35)*mesh.fade);
            vertex(out,matrix,a,r,g,bl,alpha); vertex(out,matrix,b,r,g,bl,alpha);
            vertex(out,matrix,c,r,g,bl,alpha); vertex(out,matrix,d,r,g,bl,alpha);
        }
    }
    private static Vec3 point(Vec3[] q,double u,double v) { return q[0].lerp(q[1],u).lerp(q[3].lerp(q[2],u),v); }
    private static void vertex(BufferBuilder out,Matrix4f matrix,Vec3 p,int r,int g,int b,int a) {
        out.vertex(matrix,(float)p.x,(float)p.y,(float)p.z).color(r,g,b,a).endVertex();
    }
    private static final class Capture implements MultiBufferSource {
        final List<Vec3[]> quads=new ArrayList<>();
        final Set<Integer> seen=new HashSet<>();
        final Map<RenderType,VertexConsumer> consumers=new HashMap<>();
        @Override public VertexConsumer getBuffer(RenderType type) {
            return consumers.computeIfAbsent(type,t->new Collector(this,t.format()==DefaultVertexFormat.NEW_ENTITY && t.mode()==VertexFormat.Mode.QUADS));
        }
    }
    private static final class Collector implements VertexConsumer {
        final Capture capture; final boolean enabled;
        Vec3[] quad=new Vec3[4]; int count; double x,y,z;
        Collector(Capture capture,boolean enabled) { this.capture=capture; this.enabled=enabled; }
        @Override public VertexConsumer vertex(double x,double y,double z) { this.x=x;this.y=y;this.z=z;return this; }
        @Override public void endVertex() {
            if(!enabled || capture.quads.size()>=2048) return;
            quad[count++]=new Vec3(x,y,z);
            if(count==4) {
                if(capture.seen.add(Arrays.hashCode(quad))) capture.quads.add(quad);
                quad=new Vec3[4]; count=0;
            }
        }
        @Override public VertexConsumer color(int r,int g,int b,int a) { return this; }
        @Override public VertexConsumer uv(float u,float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u,int v) { return this; }
        @Override public VertexConsumer uv2(int u,int v) { return this; }
        @Override public VertexConsumer normal(float x,float y,float z) { return this; }
        @Override public void defaultColor(int r,int g,int b,int a) {}
        @Override public void unsetDefaultColor() {}
    }
}
