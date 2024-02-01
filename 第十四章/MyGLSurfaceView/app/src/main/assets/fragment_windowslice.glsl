uniform sampler2D uSampler;
uniform sampler2D uDstSampler;
varying vec2 vTexCoord;

uniform float uProgress;
float count = 10.0;
float smoothness = 0.8;

vec4 getFromColor(vec2 uv){
    return texture2D(uSampler, uv);
}

vec4 getDstColor(vec2 uv){
    return texture2D(uDstSampler, uv);
}

vec4 transition(vec2 uv){
    vec4 a = getFromColor(uv);
    vec4 b = getDstColor(uv);
    float pr = smoothstep(-smoothness, 0.0, uv.x - uProgress * (1.0 + smoothness));
    float c = step(pr, fract(count * uv.x));
    return mix(a, b, c);
}

void main() {
    gl_FragColor = transition(vTexCoord);

}
