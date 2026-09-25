package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid=Oasiso.MODID,value=Dist.CLIENT)
public final class ApollyonSpearImpact {
    private static final Map<ApollyonEntity,Cloud> CLOUDS=new WeakHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFER=MultiBufferSource.immediate(new BufferBuilder(16384));
    private static ClientLevel world;
    private static Matrix4f view,projection;
    private static ShaderInstance shader;
    private ApollyonSpearImpact() {}
    private static void checkWorld() {
        if(world!=Minecraft.getInstance().level){world=Minecraft.getInstance().level;CLOUDS.clear();view=projection=null;}
    }
    public static void sample(ApollyonEntity entity,Vec3 tip) {
        checkWorld();
        if(world==null || entity.level()!=world || !entity.isPushingPlayer() || entity.isTeleporting())return;
        if(!Double.isFinite(tip.lengthSqr()))return;
        Cloud cloud=CLOUDS.computeIfAbsent(entity,e -> new Cloud(e.getId(),world.getGameTime()));
        if(cloud.tip!=null && cloud.tip.distanceToSqr(tip)>64)cloud.sparks.clear();
        if(world.getGameTime()-cloud.lastSeen>3)cloud.began=world.getGameTime();
        cloud.tip=tip;cloud.lastSeen=world.getGameTime();
    }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        checkWorld();
        if(world==null || Minecraft.getInstance().isPaused())return;
        long now=world.getGameTime();
        var entries=CLOUDS.entrySet().iterator();
        while(entries.hasNext()) {
            var entry=entries.next();var entity=entry.getKey();Cloud cloud=entry.getValue();
            cloud.sparks.removeIf(s -> now-s.born>=s.life);
            if(entity.isRemoved() || !entity.isAlive() || entity.isTeleporting()
                    || (now-cloud.lastSeen>12 && cloud.sparks.isEmpty())) {entries.remove();continue;}
            if(entity.isPushingPlayer() && now-cloud.lastSeen<=2 && now%4==0) {
                var player=world.getEntity(entity.getPushedPlayerId());
                if(player!=null && player.onGround()) {
                    var smoke=Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            player.getX()+(cloud.random.nextDouble()-.5)*.35,player.getY()+.04,
                            player.getZ()+(cloud.random.nextDouble()-.5)*.35,0,.003,0);
                    if(smoke!=null){smoke.scale(.25F);smoke.setLifetime(14+cloud.random.nextInt(6));}
                }
            }
            if(entity.isPushingPlayer() && now-cloud.lastSeen<=2 && cloud.tip!=null && now%2==0) {
                for(int i=0;i<3 && cloud.sparks.size()<36;i++) {
                    RandomSource r=cloud.random;
                    Vec3 direction=new Vec3(r.nextDouble()*2-1,r.nextDouble()*1.4-.5,r.nextDouble()*2-1).normalize();
                    cloud.sparks.add(new Spark(cloud.tip,direction.scale(.14+r.nextDouble()*.14),
                            .24F+r.nextFloat()*.22F,.10F+r.nextFloat()*.16F,now,6+r.nextInt(5),r.nextInt(256)));
                }
            }
        }
    }
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            checkWorld();view=new Matrix4f(event.getPoseStack().last().pose());
            projection=new Matrix4f(RenderSystem.getProjectionMatrix());return;
        }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        Matrix4f matrix=view,proj=projection;view=projection=null;
        Minecraft mc=Minecraft.getInstance();
        if(shader==null || world==null || world!=mc.level || matrix==null || proj==null || CLOUDS.isEmpty())return;
        float partial=mc.isPaused()?mc.getFrameTime():event.getPartialTick();
        double now=world.getGameTime()+partial;
        Vec3 eye=event.getCamera().getPosition();
        Vector3f r=new Vector3f(1,0,0).rotate(event.getCamera().rotation());
        Vector3f u=new Vector3f(0,1,0).rotate(event.getCamera().rotation());
        Vec3 right=new Vec3(r.x,r.y,r.z),up=new Vec3(u.x,u.y,u.z);
        shader.safeGetUniform("ImpactProjection").set(proj);
        shader.safeGetUniform("Time").set((float)(now%24000)/20F);
        ShaderInstance previous=RenderSystem.getShader();
        VertexConsumer out=BUFFER.getBuffer(ImpactType.TYPE);
        try {
            int count=0;
            for(var entry:CLOUDS.entrySet()) {
                var entity=entry.getKey();Cloud cloud=entry.getValue();
                if(entity.isRemoved() || !entity.isAlive() || entity.isInvisible() || entity.isTeleporting()
                        || cloud.tip==null || cloud.tip.distanceToSqr(eye)>64*64)continue;
                if(count++>=24)break;
                float fade=Mth.clamp((float)((64-cloud.tip.distanceTo(eye))/8),0,1);
                for(Spark spark:cloud.sparks) {
                    float age=(float)(now-spark.born),t=age/spark.life;
                    if(t<0 || t>=1)continue;
                    Vec3 a=spark.from.add(spark.velocity.scale(age)).add(0,-.004*age*age,0);
                    Vec3 b=a.add(spark.velocity.normalize().scale(spark.length*(1-t*.4)));
                    Vec3 side=b.subtract(a).cross(a.subtract(eye));
                    if(side.lengthSqr()<1E-8)side=right;
                    side=side.normalize().scale(spark.width*.5);
                    quad(out,matrix,eye,a.subtract(side),a.add(side),b.add(side),b.subtract(side),
                            255,spark.color,Math.round(255*fade*(1-t)));
                }
                if(entity.isPushingPlayer() && now-cloud.lastSeen<2) {
                    float radius=.48F*(1+.10F*(float)Math.sin(now*.8));
                    Vec3 x=right.scale(radius),y=up.scale(radius);
                    int alpha=Math.round(255*fade*Mth.clamp((float)(now-cloud.began)/3,0,1));
                    quad(out,matrix,eye,cloud.tip.subtract(x).subtract(y),cloud.tip.add(x).subtract(y),
                            cloud.tip.add(x).add(y),cloud.tip.subtract(x).add(y),0,entity.getId()&255,alpha);
                }
            }
        } finally {
            BUFFER.endBatch(ImpactType.TYPE);
            if(previous!=null)RenderSystem.setShader(() -> previous);
        }
    }
    private static void quad(VertexConsumer out,Matrix4f view,Vec3 eye,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int kind,int color,int alpha) {
        vertex(out,view,eye,a,0,0,kind,color,alpha);vertex(out,view,eye,b,1,0,kind,color,alpha);
        vertex(out,view,eye,c,1,1,kind,color,alpha);vertex(out,view,eye,d,0,1,kind,color,alpha);
    }
    private static void vertex(VertexConsumer out,Matrix4f view,Vec3 eye,Vec3 p,float u,float v,int kind,int color,int alpha) {
        out.vertex(view,(float)(p.x-eye.x),(float)(p.y-eye.y),(float)(p.z-eye.z))
                .color(kind,color,255,alpha).uv(u,v).endVertex();
    }
    private record Spark(Vec3 from,Vec3 velocity,float length,float width,long born,int life,int color) {}
    private static final class Cloud {
        final List<Spark> sparks=new ArrayList<>();final RandomSource random;
        Vec3 tip;long lastSeen,began;
        Cloud(int seed,long now){random=RandomSource.create(seed*7349L+now);lastSeen=began=now;}
    }

    public static void registerShader(RegisterShadersEvent event)throws IOException {
        shader=null;CLOUDS.clear();view=projection=null;
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Oasiso.MODID,"apollyon_spear_impact"),
                DefaultVertexFormat.POSITION_COLOR_TEX),s -> shader=s);
    }
    private static final class ImpactType extends RenderType {
        static final RenderType TYPE=create("oasiso_apollyon_spear_impact",DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,16384,false,true,CompositeState.builder()
                        .setShaderState(new ShaderStateShard(() -> shader)).setCullState(NO_CULL)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        private ImpactType(String n,VertexFormat f,VertexFormat.Mode m,int size,boolean c,boolean sort,Runnable setup,Runnable clear) {
            super(n,f,m,size,c,sort,setup,clear);
        }
    }
}
