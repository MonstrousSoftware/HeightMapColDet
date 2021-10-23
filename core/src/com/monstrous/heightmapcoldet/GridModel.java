package com.monstrous.heightmapcoldet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GridModel {

    public int gridSize = 64+1;
    public float gridScale = 400f;      // world scale of whole grid (x and z)
    public float amplitude = 40f;       // height multiplier
    public float minHeight = 999f;
    public float maxHeight = -999f;
    private Noise noise;
    private Model model;
    private FloatBuffer rawData = null;

    public GridModel() {
        noise = new Noise();
        model = makeGridModel(gridScale, gridSize-1, GL20.GL_LINES, new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)));
    }

    public Model getModel() {
        return model;
    }

    // convert float array to a FloatBuffer
    private FloatBuffer toFloatBuffer(float[] v ) {
        ByteBuffer buf = ByteBuffer.allocateDirect(v.length*4);
        buf.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = buf.asFloatBuffer();
        buffer.put(v);
        buffer.position(0);
        return buffer;
    }

    // for use in Bullet's btHeightfieldTerrainShape()
    public FloatBuffer getRawData() {
        if(rawData == null)
        {
            float data[] = new float[gridSize*gridSize];
            final int N = gridSize-1;
            for (int y = 0; y <= N; y++) {
                float posy = ((float) y / (float) N) - 0.5f;        // y in [-0.5f .. 0.5f]
                for (int x = 0; x <= N; x++) {
                    float posx = ((float) x / (float) N - 0.5f);        // x in [-0.5f .. 0.5f]

                    float posz = getHeight(posx, posy);
                    data[y*gridSize+x] = posz;                  // Bullet expects y major
                }
            }
            rawData = toFloatBuffer(data);
        }
        return rawData;
    }

    // make a Model consisting of a square grid
    public Model makeGridModel(float scale, int divisions, int primitive, Material material) {
        final int N = divisions;

        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshBuilder meshBuilder = (MeshBuilder) modelBuilder.part("face", primitive, attr, material);
        final int numVerts = (N + 1) * (N + 1);
        final int numTris = 2 * N * N;
        Vector3 vertices[] = new Vector3[numVerts];
        Vector3 normals[] = new Vector3[numVerts];


        meshBuilder.ensureVertices(numVerts);
        meshBuilder.ensureTriangleIndices(numTris);

        Vector3 pos = new Vector3();
        float posz;

        for (int y = 0; y <= N; y++) {
            float posy = ((float) y / (float) N) - 0.5f;        // y in [-0.5f .. 0.5f]
            for (int x = 0; x <= N; x++) {
                float posx = ((float) x / (float) N - 0.5f);        // x in [-0.5f .. 0.5f]

                posz = getHeight(posx, posy);
                pos.set(posx*scale, posz, posy*scale);			// swapping z,y to orient horizontally


                vertices[y * (N + 1) + x] = new Vector3(pos);
                normals[y * (N + 1) + x] = new Vector3(0,0,0);


            }
            if (y >= 1) {
                // add to index list to make a row of triangles using vertices at y and y-1
                short v0 = (short) ((y - 1) * (N + 1));    // vertex number at top left of this row
                for (short t = 0; t < N; t++) {
                    // counter-clockwise winding
                    addTriangle(meshBuilder, vertices, normals, N, v0,  (short) (v0 + N + 1), (short) (v0 + 1));
                    addTriangle(meshBuilder, vertices, normals, N, (short) (v0 + 1), (short) (v0 + N + 1), (short) (v0 + N + 2));
                    v0++;                // next column
                }
            }
        }

        // now normalize each normal (which is the sum of the attached triangle normals)
        // and pass vertex to meshBuilder
        MeshPartBuilder.VertexInfo vert = new MeshPartBuilder.VertexInfo();
        vert.hasColor = false;
        vert.hasNormal = true;
        vert.hasPosition = true;
        vert.hasUV = true;

        Vector3 normal = new Vector3();
        for (int i = 0; i < numVerts; i++) {
            normal.set(normals[i]);
            normal.nor();


            int x = i % (N+1);	// e.g. in [0 .. 3] if N == 3
            int y = i / (N+1);
            float reps=16;
            float u = (float)(x*reps)/(float)(N+1);
            float v = (float)(y*reps)/(float)(N+1);
            vert.position.set(vertices[i]);
            vert.normal.set(normal);
            vert.uv.x = u;					// texture needs to have repeat wrapping enables to handle u,v > 1
            vert.uv.y = v;
            meshBuilder.vertex(vert);
        }

        Model model = modelBuilder.end();
        return model;
    }

    private void addTriangle(MeshBuilder meshBuilder, final Vector3[] vertices, Vector3[] normals, int N, short v0, short v1, short v2) {
        meshBuilder.triangle(v0, v1, v2);
        calcNormal(vertices, normals, v0, v1, v2);
    }

    /*
     * Calculate the normal
     */
    private void calcNormal(final Vector3[] vertices, Vector3[] normals, short v0, short v1, short v2) {

        Vector3 p0 = vertices[v0];
        Vector3 p1 = vertices[v1];
        Vector3 p2 = vertices[v2];

        Vector3 u = new Vector3();
        u.set(p2);
        u.sub(p0);

        Vector3 v = new Vector3();
        v.set(p1);
        v.sub(p0);

        Vector3 n = new Vector3();
        n.set(v);
        n.crs(u);
        n.nor();

        normals[v0].add(n);
        normals[v1].add(n);
        normals[v2].add(n);
    }

    public float getHeight( float x, float y ) {
        float sc = 5f;
        float h = noise.PerlinNoise(x*sc, y*sc);
        h*= amplitude;
        if(h > maxHeight)
            maxHeight = h;
        if(h < minHeight)
            minHeight = h;
        return h;
    }

}
