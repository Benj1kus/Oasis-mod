#version 150
uniform sampler2D Scene;
uniform sampler2D Depth;
uniform sampler2D Shadows;
uniform mat4 InverseViewProjection;
uniform vec2 Resolution;
uniform vec3 CameraCell;
uniform float Time;
uniform int Count;
// xyz = положение относительно камеры, w = радиус света.
uniform vec4 LightPosition[4];
// rgb = основной цвет, w = сила света с учётом появления/дистанции.
uniform vec4 LightColor[4];
// rgb = второй цвет, w = радиус дымки.
uniform vec4 LightAccent[4];
// x = плотность дымки, y = ореол, z = плавное появление, w = seed.
uniform vec4 LightSettings[4];

in vec2 texCoord;
out vec4 fragColor;

const float SURFACE_PIXELS_PER_BLOCK = 12.0;
const float SHADE_STEPS = 7.0;
const float FOG_PIXEL_SIZE = 3.0; // экранные пиксели при высоте кадра 1080
const float SHADOW_SIZE = 16.0; // совпадает с SHADOW_SIZE в Java
const int FOG_SAMPLES = 8;

float bayer2(vec2 p) {
    p=mod(floor(p),2.0);
    return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;
}
float bayer4(vec2 p) {
    return (4.0*bayer2(p)+bayer2(floor(p*0.5))+0.5)/16.0;
}
float hash(vec3 p) {
    p=fract(p*0.1031);
    p+=dot(p,p.yzx+33.33);
    return fract((p.x+p.y)*p.z);
}
float noise(vec3 p) {
    vec3 cell=floor(p),f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash(cell),hash(cell+vec3(1,0,0)),f.x),
                   mix(hash(cell+vec3(0,1,0)),hash(cell+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash(cell+vec3(0,0,1)),hash(cell+vec3(1,0,1)),f.x),
                   mix(hash(cell+vec3(0,1,1)),hash(cell+vec3(1,1,1)),f.x),f.y),f.z);
}
vec3 reconstruct(vec2 uv,float depth) {
    vec4 p=InverseViewProjection*vec4(uv*2.0-1.0,depth*2.0-1.0,1.0);
    return p.xyz/p.w;
}
float quantize(float value,float steps,float dither) {
    float s=max(value,0.0)*steps;
    return (floor(s)+step(dither,fract(s)))/steps;
}

// Небольшая карта расстояний вокруг источника: учитывает и стены вне экрана.
float visibility(int index,vec3 offset) {
    float distance=length(offset);
    if(distance<0.12) return 1.0;
    vec3 a=abs(offset);
    float face; vec2 uv;
    if(a.x>=a.y && a.x>=a.z) {
        face=offset.x>0.0?0.0:1.0;
        uv=vec2(offset.x>0.0?-offset.z:offset.z,-offset.y)/a.x;
    } else if(a.y>=a.z) {
        face=offset.y>0.0?2.0:3.0;
        uv=vec2(offset.x,offset.y>0.0?offset.z:-offset.z)/a.y;
    } else {
        face=offset.z>0.0?4.0:5.0;
        uv=vec2(offset.z>0.0?offset.x:-offset.x,-offset.y)/a.z;
    }
    vec2 cell=clamp(floor((uv*0.5+0.5)*SHADOW_SIZE),vec2(0),vec2(SHADOW_SIZE-1.0));
    vec2 atlas=(vec2(face*SHADOW_SIZE,float(index)*SHADOW_SIZE)+cell+0.5)
            /vec2(SHADOW_SIZE*6.0,SHADOW_SIZE*4.0);
    float wall=texture(Shadows,atlas).r;
    // Допуск для дискретной карты: сами освещаемые грани не затеняют себя.
    float bias=0.09+distance*0.038;
    return 1.0-smoothstep(wall+bias,wall+bias+0.10,distance);
}

// Нормаль из глубины. Выбираем соседа с меньшим скачком,
// чтобы силуэт блока не создавал длинные ложные блики.
vec3 surfaceNormal(vec3 p) {
    vec2 px=1.0/Resolution;
    vec3 r=reconstruct(texCoord+vec2(px.x,0),texture(Depth,texCoord+vec2(px.x,0)).r)-p;
    vec3 l=p-reconstruct(texCoord-vec2(px.x,0),texture(Depth,texCoord-vec2(px.x,0)).r);
    vec3 u=reconstruct(texCoord+vec2(0,px.y),texture(Depth,texCoord+vec2(0,px.y)).r)-p;
    vec3 d=p-reconstruct(texCoord-vec2(0,px.y),texture(Depth,texCoord-vec2(0,px.y)).r);
    vec3 n=cross(dot(r,r)<dot(l,l)?r:l,dot(u,u)<dot(d,d)?u:d);
    float len=length(n);
    if(len<0.000001) return -normalize(p);
    n/=len;
    return dot(n,-p)<0.0?-n:n;
}

void main() {
    vec4 scene=texture(Scene,texCoord);
    float depth=texture(Depth,texCoord).r;
    bool surface=depth<0.999999;
    vec3 p=reconstruct(texCoord,min(depth,0.99999));
    vec3 normal=vec3(0);
    bool needNormal=false;
    for(int i=0;i<4;++i) {
        if(i>=Count) break;
        if(surface && distance(p,LightPosition[i].xyz)<LightPosition[i].w) needNormal=true;
    }
    if(needNormal) normal=surfaceNormal(p);
    vec3 grid=floor((p+CameraCell)*SURFACE_PIXELS_PER_BLOCK);
    vec3 shadePoint=(grid+0.5)/SURFACE_PIXELS_PER_BLOCK-CameraCell;
    vec3 absNormal=abs(normal);
    vec2 surfaceCell=absNormal.y>absNormal.x && absNormal.y>absNormal.z?grid.xz:
                     (absNormal.x>absNormal.z?grid.zy:grid.xy);
    float surfaceDither=bayer4(surfaceCell);

    // Только дымка пикселизируется в экранных координатах; сами блоки не размываются.
    float pixel=max(1.0,floor(FOG_PIXEL_SIZE*Resolution.y/1080.0));
    vec2 fogCell=floor(texCoord*Resolution/pixel);
    vec2 fogUV=(fogCell+0.5)*pixel/Resolution;
    vec3 ray=normalize(reconstruct(fogUV,0.99999));
    float fogDepth=texture(Depth,fogUV).r;
    // Берём ближайшую глубину из центрального и текущего пикселя, чтобы дымка не текла через края стен.
    float stop=surface?length(p):10000.0;
    if(fogDepth<0.999999) stop=min(stop,length(reconstruct(fogUV,fogDepth)));
    float fogDither=bayer4(fogCell);
    vec3 added=vec3(0),fogColor=vec3(0);
    float fogAlpha=0.0;

    for(int i=0;i<4;++i) {
        if(i>=Count) break;
        vec3 light=LightPosition[i].xyz;
        float radius=LightPosition[i].w;
        vec3 primary=LightColor[i].rgb,accent=LightAccent[i].rgb;
        float strength=LightColor[i].a;
        float seed=LightSettings[i].w;
        float flicker=0.95+0.035*sin(Time*2.2+seed)+0.015*sin(Time*5.1+seed*2.0);
        vec3 delta=shadePoint-light;
        float d=length(delta);
        if(surface && d<radius) {
            float falloff=pow(max(0.0,1.0-d/radius),1.65);
            float facing=max(0.0,dot(normal,-delta/max(d,0.001)));
            falloff*=mix(0.12,1.0,facing)*visibility(i,delta);
            falloff=quantize(falloff,SHADE_STEPS,surfaceDither);
            vec3 tint=mix(primary,accent,0.06+0.07*sin(p.y*1.2+Time*0.35+seed));
            // Сохраняем рисунок уже отрендеренной поверхности.
            added+=tint*(vec3(0.19)+scene.rgb*0.68)*falloff*strength*flicker;
        }

        float fogRadius=LightAccent[i].a;
        if(fogRadius<=0.0) continue;
        float along=dot(light,ray);
        float sideways=dot(light,light)-along*along;
        float discriminant=fogRadius*fogRadius-sideways;
        if(discriminant<=0.0) continue;
        float halfChord=sqrt(discriminant);
        float begin=max(0.0,along-halfChord),end=min(stop,along+halfChord);
        if(end<=begin) continue;
        float stepLength=(end-begin)/float(FOG_SAMPLES);
        float density=0.0;
        for(int j=0;j<FOG_SAMPLES;++j) {
            vec3 point=ray*(begin+(float(j)+0.5)*stepLength);
            vec3 rel=point-light;
            float radial=max(0.0,1.0-length(rel)/fogRadius);
            vec3 flow=rel*1.55+vec3(seed, -Time*0.28, Time*0.11);
            float mist=mix(0.40,1.0,noise(flow));
            density+=radial*radial*mist*visibility(i,rel)*stepLength;
        }
        float alpha=(1.0-exp(-density*LightSettings[i].x))*LightSettings[i].z;
        alpha=quantize(alpha,24.0,fogDither);
        float colorFlow=0.16+0.19*sin(Time*0.45+seed+fogCell.y*0.025);
        vec3 mistColor=mix(primary,accent,clamp(colorFlow,0.0,0.4));
        fogColor+=mistColor*alpha;
        fogAlpha+=alpha;

        // Мягкий локальный ореол, ограниченный глубиной сцены и преградами.
        // Это свечение источника, а не размытие всего кадра/интерфейса.
        if(along>0.0 && along<stop+0.12) {
            vec3 closest=ray*along-light;
            float glow=exp(-max(sideways,0.0)/0.38)*visibility(i,closest);
            glow*=LightSettings[i].y*LightSettings[i].z*flicker;
            added+=mix(primary,accent,0.1)*quantize(glow,24.0,fogDither);
        }
    }
    float a=min(fogAlpha,0.32);
    vec3 mist=fogAlpha>0.0001?fogColor/fogAlpha:vec3(0);
    vec3 result=mix(scene.rgb+added,mist,a);
    fragColor=vec4(clamp(result,0.0,1.0),scene.a);
}
