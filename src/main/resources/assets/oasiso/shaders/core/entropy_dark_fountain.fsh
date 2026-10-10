#version 150
uniform float Time;
uniform float Seed;
uniform float Reveal;
uniform vec2 Dimensions;
uniform int BlobCount;
uniform vec4 Blobs[28];
in vec2 texCoord;
in float cameraDistance;
in float vertexAlpha;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7))+Seed)*43758.5453); }
float bayer2(vec2 p) {
    p=mod(floor(p),2.0);
    return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;
}
float bayer4(vec2 p) { return (4.0*bayer2(p)+bayer2(floor(p*.5))+.5)/16.0; }
float ball(vec2 delta,vec2 radii,float kind) {
    vec2 d=delta/radii;
    float r=dot(d,d);
    // A few pointed/diamond-shaped droplets among the softer blobs.
    r=mix(r,pow(abs(d.x)+abs(d.y)*.82,2.0),kind);
    float support=max(0.0,1.0-r);
    return support*support*2.5;
}
void main() {
    // Physical pixel size stays consistent for both a single block and a 5x5 fountain.
    float pixel=1.0/20.0;
    vec2 physical=vec2((texCoord.x-.5)*Dimensions.x,texCoord.y*Dimensions.y);
    vec2 cell=floor(physical/pixel);
    vec2 p=(cell+.5)*pixel;
    float dither=bayer4(cell);
    vec2 q=vec2(p.x/(Dimensions.x*.5),p.y);
    // Flames pull the contour into thin moving tongues; inner matter moves more slowly.
    q.x+=.065*sin(q.y*6.0-Time*2.3+Seed)+.026*sin(q.y*13.0-Time*3.2);
    q.y+=.035*sin(q.x*19.0+Time*1.4);
    float field=0.0;
    for(int i=0;i<28;i++) {
        if(i>=BlobCount)break;
        vec4 b=Blobs[i];
        float lean=.16*sin(Time*.5+float(i)*1.7);
        vec2 delta=q-b.xy;
        delta.x+=delta.y*lean;
        field+=ball(delta,b.zw,(i%4==0)?.5:0.0);
    }
    // Small pools at both emitters keep the stream attached to floor and ceiling.
    float poolHeight=min(.70,Dimensions.y*.34);
    field+=ball(q-vec2(0,-poolHeight*.24),vec2(.78,poolHeight),0.0);
    field+=ball(q-vec2(0,Dimensions.y+poolHeight*.24),vec2(.78,poolHeight),0.0);
    float threshold=.48+.035*sin(Time*2.1+p.y*5.0);
    if(field<threshold)discard;

    float interior=smoothstep(.95,1.48,field);
    vec2 swirl=vec2(q.x*1.5,q.y*.7);
    swirl.x+=.21*sin(swirl.y*2.3-Time*.32);
    float spiral=sin(atan(swirl.y-Dimensions.y*.35,swirl.x+.001)*3.0
            -length(vec2(swirl.x,swirl.y-Dimensions.y*.35))*6.0+Time*.55);
    spiral+=.6*sin(q.y*3.4+q.x*5.5-Time*.4);
    vec3 darkBlue=vec3(.022,.032,.095);
    vec3 darkPurple=vec3(.095,.045,.23);
    vec3 blue=vec3(.045,.10,.29);
    float band=floor(clamp(spiral*.35+.5,0.0,1.0)*4.0+dither)/4.0;
    vec3 dark=mix(darkBlue,darkPurple,band);
    dark=mix(dark,blue,step(.72,fract(q.y*.24+q.x*.7-Time*.035))*.45);

    // Groups of five crisp stars appear, twinkle and vanish inside the dark material.
    float group=floor(Time/3.6);
    float phase=fract(Time/3.6);
    float envelope=smoothstep(.08,.24,phase)*(1.0-smoothstep(.65,.91,phase));
    int groupIndex=int(mod(group*7.0+mod(Seed,19.0),float(max(BlobCount,1))));
    vec2 groupCenter=vec2(Blobs[groupIndex].x*Dimensions.x*.5,
                         clamp(Blobs[groupIndex].y,.05*Dimensions.y,.95*Dimensions.y));
    float stars=0.0;
    for(int i=0;i<5;i++) {
        float f=float(i);
        vec2 offset=vec2((hash(vec2(group+f,31))-.5)*min(.85,Dimensions.x*.45),
                         (hash(vec2(group+f,43))-.5)*min(1.2,Dimensions.y*.4));
        vec2 c=floor((groupCenter+offset)/pixel)*pixel+pixel*.5;
        vec2 d=abs(p-c)/pixel;
        float arm=2.0+floor(hash(vec2(f,group))*2.0);
        float crossShape=max((1.0-step(.51,d.x))*(1.0-step(arm,d.y)),
                             (1.0-step(.51,d.y))*(1.0-step(arm,d.x)));
        float twinkle=.4+.6*pow(.5+.5*sin(Time*7.0+f*2.2),2.0);
        stars=max(stars,crossShape*twinkle*envelope);
    }
    dark=mix(dark,vec3(.65,.98,1.0),stars*step(1.2,field));
    float gradient=.5+.5*sin(q.y*.85+Time*.45+q.x*2.0+Seed*.1);
    gradient=floor(gradient*7.0+dither)/7.0;
    vec3 edge=mix(vec3(.14,.91,1.0),vec3(1.0,.19,.69),gradient);
    edge=mix(edge,vec3(.74,.98,1.0),.12+.10*sin(Time*3.0+q.y*2.0));
    vec3 color=mix(edge,dark,step(dither,interior));
    float alpha=mix(.82,.99,interior)*Reveal*vertexAlpha;
    alpha*=smoothstep(.14,.7,cameraDistance);
    if(alpha<.008)discard;
    fragColor=vec4(color,alpha);
}
