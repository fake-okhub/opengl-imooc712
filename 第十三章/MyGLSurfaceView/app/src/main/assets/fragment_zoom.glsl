uniform sampler2D uSampler;
uniform sampler2D uDstSampler;
varying vec2 vTexCoord;

uniform float uProgress;

vec4 getFromColor(vec2 uv){
    return texture2D(uSampler, uv);
}

vec4 getDstColor(vec2 uv){
    return texture2D(uDstSampler, uv);
}

vec2 zoom(vec2 uv, float amount){
    return 0.5 + ((uv - 0.5) * (1.0 - amount));
}

vec4 transition(vec2 uv){
    vec4 a = getFromColor(zoom(uv, uProgress));
    vec4 b = getDstColor(uv);

    return mix(a, b, smoothstep(0.6, 0.8, uProgress));
}

void main() {
    gl_FragColor = transition(vTexCoord);
}