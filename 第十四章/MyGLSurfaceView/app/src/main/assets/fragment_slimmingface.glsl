uniform sampler2D uSampler;
varying vec2 vTexCoord;
uniform vec2 uImageSize;

uniform vec2 uLeftCheekOne;
uniform vec2 uLeftCheekTwo;
uniform vec2 uRightCheekOne;
uniform vec2 uRightCheekTwo;

uniform vec2 uNoseOne;
uniform vec2 uNoseTwo;

vec2 slimmingFace(vec2 curPos, vec2 originPos, vec2 directPos, float alpha){
    vec2 result;
    result = curPos * uImageSize;

    vec2 radius = (directPos - originPos) * 0.5;
    vec2 targetPos = originPos + radius;
    float rMax = distance(targetPos, originPos);
    float r = distance(result, originPos);
    if(r < rMax){
        float k = pow(1.0 - r / rMax, 2.0) * alpha;
        result = result - k * radius;
    }
    result = result / uImageSize;

    return result;
}

void main() {
    vec2 np = slimmingFace(vTexCoord, uLeftCheekOne + vec2(-5.0, 5.0), uNoseOne, 0.2);
    np = slimmingFace(np, uLeftCheekTwo + vec2(-5.0, 5.0), uNoseTwo, 0.2);

    np = slimmingFace(np, uRightCheekOne + vec2(5.0, 5.0), uNoseOne, 0.2);
    np = slimmingFace(np, uRightCheekTwo + vec2(5.0, 5.0), uNoseTwo, 0.2);

    gl_FragColor = texture2D(uSampler, np);
}
