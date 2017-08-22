package org.tudelft.ewi.ce.fpgaserialize;

public class KMVectorJava {
    public int size;
    public float[] values;

    KMVectorJava(int size, float[] values) {
        this.size = size;
        this.values = values;
    }

    @Override public String toString() {
        StringBuffer strBuf = new StringBuffer(10*size);
        for (int v = 0; v < size; v++) {
            strBuf.append(Float.toString(values[v]) + ", ");
        }
        return strBuf.toString();
    }

    private void clear() {
        for (int i = 0; i < size;i++)
            values[i] = 0.0f;
    }

    private void add(KMVectorJava a) {
        if (a.size == size)
            for (int i = 0; i < size;i++)
                values[i] += a.values[i];
        else
            throw new Error("Point dimensions must be the same.");
    }

    private void scale (float factor) {
        for (int i = 0; i < size;i++)
            values[i] = values[i] / factor;
    }

    private KMVectorJava copy() {
        KMVectorJava ret = new KMVectorJava(size, new float[size]);
        ret.add(this);
        return ret;
    }

    private static float calculateSquaredDistance(KMVectorJava a, KMVectorJava b) {
        if (a.size == b.size) {
            float sum = 0.0f;
            for (int i = 0; i < a.size; i++) {
                sum += (a.values[i] - b.values[i]) * (a.values[i] - b.values[i]);
            }
            return sum;
        }
        else
            throw new Error("Point dimensions not equal.");
    }

    private static float assignPoints (int numPoints, int numCentroids, KMVectorJava[] points, int[] assignments, KMVectorJava[] centroids) {
        float delta = 0.0f;
        for (int p = 0; p < numPoints; p++) {
            float tempDist = Float.POSITIVE_INFINITY;
            int tempAssignment = assignments[p];
            for (int c = 0; c < numCentroids; c++) {
                float curDist = calculateSquaredDistance(points[p], centroids[c]);
                if (curDist < tempDist) {
                    assignments[p] = c;
                    tempDist = curDist;
                }
            }
            if (assignments[p] != tempAssignment) {
                delta = delta + 1.0f;
            }
        }
        return delta;
    }

    private static void moveCentroids(int numPoints, int numCentroids, KMVectorJava[] vecs, int[] assignments, KMVectorJava[] centroids) {
        int[] pointsPerCentroid = new int[numCentroids];

        for (int c = 0; c < numCentroids; c++) {
            centroids[c].clear();
        }

        for (int p = 0; p < numPoints; p++) {
            centroids[assignments[p]].add(vecs[p]);
            pointsPerCentroid[assignments[p]] += 1;
        }

        for (int c = 0; c < numCentroids; c++) {
            centroids[c].scale((float)pointsPerCentroid[c]);
        }
    }

    public static int cluster(KMVectorJava[] vecs, int objects, int centers, int repeats, float threshold) {
        int i = 0;
        for (int r = 0; r < repeats; r++) {
            int[] assignments = new int[objects];
            KMVectorJava[] centroids = new KMVectorJava[centers];
            for (int c = 0; c < centers; c++) {
                centroids[c] = vecs[c].copy();
            }
            float delta = Float.POSITIVE_INFINITY;
            i = 1;
            //PrintIteration(objects, centers, vecs, assignments, centroids);
            do {
                delta = assignPoints(objects, centers, vecs, assignments, centroids);
                moveCentroids(objects, centers, vecs, assignments, centroids);
                i = i + 1;
                //PrintIteration(objects, centers, vecs, assignments, centroids);
            } while (delta / centers > threshold);
        }
        return i;
    }

    private static void PrintIteration(int numPoints, int numCentroids, KMVectorJava[] points, int[] assignments, KMVectorJava[] centroids) {
        System.out.println(numCentroids);
        for (int c = 0; c < numCentroids; c++) {
            System.out.print(Integer.toString(c) + ", " + centroids[c].toString() + "\n");
        }
        System.out.print(Integer.toString(numPoints) + ": ");
        for (int p = 0; p < numPoints; p++) {
            System.out.print(Integer.toString(assignments[p]) + ", ");
        }
        System.out.print("\n");
    }
}
