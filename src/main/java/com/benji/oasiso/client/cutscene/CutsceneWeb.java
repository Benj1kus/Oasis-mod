package com.benji.oasiso.client.cutscene;

public final class CutsceneWeb {
    @FunctionalInterface public interface Quads {
        void quad(float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy);
    }
    private static double clamp(double x){return Math.max(0,Math.min(1,x));}
    private static double random(int seed){
        int x=seed*0x45d9f3b;x=(x^(x>>>16))*0x45d9f3b;x^=x>>>16;
        return (x&0x7fffffff)/(double)0x7fffffff;
    }
    public static void emit(int width,int height,double coverage,Quads out){
        if(coverage<=0)return;
        if(coverage>=1){out.quad(0,0,0,height,width,height,width,0);return;}
        double progress=Math.pow(clamp(coverage),1.75); // Slow aiming, then accelerating shots.
        double diagonal=Math.hypot(width,height);
        double spread=Math.pow(clamp((progress-.45)/.55),3);
        for(int strand=0;strand<36;strand++){
            int corner=strand&3;
            double sx=(corner==1||corner==2)?1.03:-.03;
            double sy=corner>=2?1.03:-.03;
            double tx=1-sx+(random(strand+91)-.5)*.6;
            double ty=1-sy+(random(strand+129)-.5)*.6;
            double delay=strand<4?strand*.015:random(strand+203)*.42;
            double head=clamp((progress-delay)/(1-delay)*1.3);
            if(head<=0)continue;
            double base=1+random(strand+33)*1.8+diagonal*spread*1.5;
            int segments=30;
            for(int i=0;i<segments;i++){
                double u=i/(double)segments,v=Math.min(head,(i+1)/(double)segments);
                if(u>=head)break;
                double ax=x(sx,sy,tx,ty,u,strand)*width,ay=y(sx,sy,tx,ty,u,strand)*height;
                double bx=x(sx,sy,tx,ty,v,strand)*width,by=y(sx,sy,tx,ty,v,strand)*height;
                double wa=base*(.1+.9*clamp((head-u)*12));
                double wb=base*(.1+.9*clamp((head-v)*12));
                ribbon(ax,ay,bx,by,wa,wb,out);
                if(i%6==3){
                    double reach=clamp((head-u)*5)*(.05+random(strand*31+i)*.09);
                    double dx=bx-ax,dy=by-ay,len=Math.max(.001,Math.hypot(dx,dy));
                    double side=((i+strand)&1)==0?1:-1;
                    double ex=ax+dx/len*diagonal*reach*.45-dy/len*diagonal*reach*side;
                    double ey=ay+dy/len*diagonal*reach*.45+dx/len*diagonal*reach*side;
                    ribbon(ax,ay,ex,ey,wa*.45,Math.max(.3,wa*.12),out);
                }
            }
        }
    }
    private static double bend(double u,int seed){
        return Math.sin(u*Math.PI)*(.07*Math.sin(u*7+random(seed+401)*6.28)
                +.022*Math.sin(u*27+random(seed+533)*6.28));
    }
    private static double x(double sx,double sy,double tx,double ty,double u,int seed){return sx+(tx-sx)*u-(ty-sy)*bend(u,seed);}
    private static double y(double sx,double sy,double tx,double ty,double u,int seed){return sy+(ty-sy)*u+(tx-sx)*bend(u,seed);}
    private static void ribbon(double ax,double ay,double bx,double by,double a,double b,Quads out){
        double dx=bx-ax,dy=by-ay,len=Math.max(.00001,Math.hypot(dx,dy));double nx=-dy/len*.5,ny=dx/len*.5;
        double ox=dx/len*.7,oy=dy/len*.7;
        out.quad((float)(ax-ox+nx*a),(float)(ay-oy+ny*a),(float)(ax-ox-nx*a),(float)(ay-oy-ny*a),
                (float)(bx+ox-nx*b),(float)(by+oy-ny*b),(float)(bx+ox+nx*b),(float)(by+oy+ny*b));
    }
    private CutsceneWeb(){}
}
