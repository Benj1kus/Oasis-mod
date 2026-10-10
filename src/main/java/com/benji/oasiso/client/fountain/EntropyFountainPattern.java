package com.benji.oasiso.client.fountain;

public final class EntropyFountainPattern {
    public static final int MAX_BLOBS=28;
    public static float[] build(double time,double height,double width,int seed) {
        int count=Math.min(MAX_BLOBS,Math.max(8,(int)Math.ceil(height*1.7)+4));
        float[] result=new float[count*4];
        for(int i=0;i<count;i++) {
            double phase=hash(seed,i,1),direction=(i%2==0?1:-1);
            double speed=(.35+hash(seed,i,2)*.38)/Math.max(2,height);
            double progress=fract(phase+direction*time*speed);
            double y=-.7+progress*(height+1.4);
            double x=(hash(seed,i,3)-.5)*.86
                    +Math.sin(time*(.35+hash(seed,i,4)*.3)+phase*6.283+y*.42)*.24;
            double rx=.20+hash(seed,i,5)*.30;
            double ry=(.30+hash(seed,i,6)*.8)*(.7+Math.sqrt(width)*.3);
            ry=Math.min(ry,Math.max(.28,height*.29));
            result[i*4]=(float)x;result[i*4+1]=(float)y;
            result[i*4+2]=(float)rx;result[i*4+3]=(float)ry;
        }
        return result;
    }
    private static double fract(double x){return x-Math.floor(x);}
    private static double hash(int seed,int index,int salt) {
        long value=seed+index*0x9E3779B97F4A7C15L+salt*0xBF58476D1CE4E5B9L;
        value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;
        value=(value^(value>>>27))*0x94D049BB133111EBL;
        return ((value^(value>>>31))>>>11)*0x1.0p-53;
    }
    private EntropyFountainPattern() {}
}
