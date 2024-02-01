uniform sampler2D uSampler;
varying vec2 vTexCoord;

uniform highp vec2 uCenterLeftEye;
uniform highp vec2 uCenterRightEye;

uniform highp vec2 uBeginLeftEye;
uniform highp vec2 uEndLeftEye;
uniform highp vec2 uBeginRightEye;
uniform highp vec2 uEndRightEye;

uniform highp vec2 uImageSize;

vec2 newCoord(vec2 curPos, vec2 centerEyePos, float rMax, float alpha) {
    vec2 result = curPos;
    vec2 imgCurPos = curPos * uImageSize;

    float r = distance(imgCurPos, centerEyePos);
    if( r < rMax){
        float k = 1.0 - pow(r/rMax - 1.0, 2.0) * alpha;
        result = k * (imgCurPos - centerEyePos) + centerEyePos;
        result = result / uImageSize;
    }

    return result;
}

void main() {
    //x_d = k * (x_c - x_0) + x_0
    //y_d = k * (y_c - y_0) + y_0

    //k = 1 - power((r / r_max  - 1), 2) * a
    //a = eyeWidth * power(512, 2) / (width * height * 100)

    float rMaxLeft = distance(uBeginLeftEye, uEndLeftEye) / 2.0;
    float rMaxRight = distance(uBeginRightEye, uEndRightEye) / 2.0;

    highp float eyeWidth = uEndRightEye.x - uBeginLeftEye.x;

    highp float a = eyeWidth * 512.0 * 512.0 / (uImageSize.x * uImageSize.y * 100.0);

    vec2 nc = newCoord(vTexCoord, uCenterLeftEye, rMaxLeft, a);
    nc = newCoord(nc, uCenterRightEye, rMaxRight, a);

    gl_FragColor = texture2D(uSampler, nc);
}
