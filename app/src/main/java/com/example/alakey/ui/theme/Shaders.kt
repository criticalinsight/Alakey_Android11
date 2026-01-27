package com.example.alakey.ui.theme

@org.intellij.lang.annotations.Language("AGSL")
const val LIQUID_PLASMA_SRC = """
    uniform float2 resolution;
    uniform float time;
    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution.xy;
        float t = time * 0.2;
        float2 p = uv * 3.0 - float2(20.0);
        float2 i = p;
        float c = 1.0;
        float inten = 0.035;
        for (int n = 0; n < 3; n++) {
            float t2 = t * (1.0 - (3.0 / float(n+1)));
            i = p + float2(cos(t2 - i.x) + sin(t2 + i.y), sin(t2 - i.y) + cos(t2 + i.x));
            c += 1.0 / length(float2(p.x / (sin(i.x+t)/inten), p.y / (cos(i.y+t)/inten)));
        }
        c /= 3.0;
        c = 1.6 - sqrt(c);
        return half4(c*c*c*c*0.14, c*c*c*0.25, c*c*0.55, 1.0);
    }
"""
