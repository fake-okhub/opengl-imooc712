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

vec4 transition(vec2 uv) {
    vec4 a = getFromColor(uv);
    vec4 b = getDstColor(uv);

    return mix(a, b, step(uv.y, uProgress));
}

void main() {
    gl_FragColor = transition(vTexCoord);
}
