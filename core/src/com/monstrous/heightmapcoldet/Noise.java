package com.monstrous.heightmapcoldet;

import com.badlogic.gdx.math.Vector2;

public class Noise {

    Vector2 a = new Vector2();
    Vector2 d1 = new Vector2();

    public Noise() {
    }


    /* Create pseudorandom direction vector
     */
    void randomGradient(int ix, int iy, Vector2 gradient) {
        final float M = 2147483648f;
        final int shift = 16;

        int a = ix;
        int b = iy;
        a *= 348234342;
        b = b ^ ((a >> shift)|(a << shift));
        b *= 933742374;
        a = a^((b >> shift)|(b << shift));
        double rnd = ((float)a/M) * Math.PI;
        gradient.set((float)Math.sin(rnd), (float)Math.cos(rnd));
    }

    float smoothstep(float a, float b, float w)
    {
        if(w < 0)
            w = 0;
        else if (w > 1.0f)
            w = 1.0f;
        float f = w*w*(3.0f-2.0f*w);
        return a + f*(b-a);
    }


    private float dotDistanceGradient(int ix, int iy, float x, float y){
        randomGradient(ix, iy, a);
        float dx = x - ix;	// distance to corner
        float dy = y - iy;
        d1.set(dx,dy);
        return a.dot(d1);
    }


    public float PerlinNoise(float x, float y) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);


        float f1 = dotDistanceGradient(ix, iy, x, y);
        float f2 = dotDistanceGradient(ix+1, iy, x, y);
        float f3 = dotDistanceGradient(ix, iy+1, x, y);
        float f4 = dotDistanceGradient(ix+1, iy+1, x, y);

        float u1 = smoothstep(f1, f2, x-ix);	// interpolate between top corners
        float u2 = smoothstep(f3, f4, x-ix);	// between bottom corners
        float res = smoothstep(u1, u2, y-iy); // between previous two points
        return res;
    }

}
