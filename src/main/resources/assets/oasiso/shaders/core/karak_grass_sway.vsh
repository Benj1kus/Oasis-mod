#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec4 normal;

uniform float ActiveCount;
uniform vec4 SmallUV;
uniform vec4 TallUV;
uniform vec4 SmallBounds;
uniform vec4 TallBounds;
uniform vec4 Plant0;
uniform vec4 Bend0;
uniform vec4 Plant1;
uniform vec4 Bend1;
uniform vec4 Plant2;
uniform vec4 Bend2;
uniform vec4 Plant3;
uniform vec4 Bend3;
uniform vec4 Plant4;
uniform vec4 Bend4;
uniform vec4 Plant5;
uniform vec4 Bend5;
uniform vec4 Plant6;
uniform vec4 Bend6;
uniform vec4 Plant7;
uniform vec4 Bend7;
uniform vec4 Plant8;
uniform vec4 Bend8;
uniform vec4 Plant9;
uniform vec4 Bend9;
uniform vec4 Plant10;
uniform vec4 Bend10;
uniform vec4 Plant11;
uniform vec4 Bend11;
uniform vec4 Plant12;
uniform vec4 Bend12;
uniform vec4 Plant13;
uniform vec4 Bend13;
uniform vec4 Plant14;
uniform vec4 Bend14;
uniform vec4 Plant15;
uniform vec4 Bend15;

bool inSprite(vec4 rect) {
    return all(greaterThanEqual(UV0,rect.xy-vec2(.0000001)))
        && all(lessThanEqual(UV0,rect.zw+vec2(.0000001)));
}
vec2 sway(vec3 pos,vec4 plant,vec4 bend,bool small,bool tall) {
    bool isTall=plant.w>.5;
    if((isTall && !tall) || (!isTall && !small)) return vec2(0.0);
    vec3 q=pos-plant.xyz;
    if(q.y<.99 || q.y>1.01) return vec2(0.0);
    vec4 bounds=isTall?TallBounds:SmallBounds;
    float xError=min(abs(q.x-bounds.x),abs(q.x-bounds.y));
    float zError=min(abs(q.z-bounds.z),abs(q.z-bounds.w));
    if(xError>.006 || zError>.006) return vec2(0.0);
    return bend.xy;
}

void main() {
    vec3 pos = Position + ChunkOffset;
    bool small=inSprite(SmallUV), tall=inSprite(TallUV);
    vec2 offset=vec2(0.0);
    if(small || tall) {
        if(ActiveCount>0.5) offset+=sway(pos,Plant0,Bend0,small,tall);
        if(ActiveCount>1.5) offset+=sway(pos,Plant1,Bend1,small,tall);
        if(ActiveCount>2.5) offset+=sway(pos,Plant2,Bend2,small,tall);
        if(ActiveCount>3.5) offset+=sway(pos,Plant3,Bend3,small,tall);
        if(ActiveCount>4.5) offset+=sway(pos,Plant4,Bend4,small,tall);
        if(ActiveCount>5.5) offset+=sway(pos,Plant5,Bend5,small,tall);
        if(ActiveCount>6.5) offset+=sway(pos,Plant6,Bend6,small,tall);
        if(ActiveCount>7.5) offset+=sway(pos,Plant7,Bend7,small,tall);
        if(ActiveCount>8.5) offset+=sway(pos,Plant8,Bend8,small,tall);
        if(ActiveCount>9.5) offset+=sway(pos,Plant9,Bend9,small,tall);
        if(ActiveCount>10.5) offset+=sway(pos,Plant10,Bend10,small,tall);
        if(ActiveCount>11.5) offset+=sway(pos,Plant11,Bend11,small,tall);
        if(ActiveCount>12.5) offset+=sway(pos,Plant12,Bend12,small,tall);
        if(ActiveCount>13.5) offset+=sway(pos,Plant13,Bend13,small,tall);
        if(ActiveCount>14.5) offset+=sway(pos,Plant14,Bend14,small,tall);
        if(ActiveCount>15.5) offset+=sway(pos,Plant15,Bend15,small,tall);
    }
    pos.xz+=offset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(ModelViewMat, pos, FogShape);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}
