package com.example.hfx;

class ColorUtils {
    private static final double REF_X = 95.047;
    private static final double REF_Y = 100.000;
    private static final double REF_Z = 108.883;

    private static final double XYZ_EPSILON = 0.008856;
    private static final double XYZ_KAPPA = 903.3;

    public static double[] rgbToLab(int r, int g, int b) {
        double[] xyz = rgbToXyz(r, g, b);
        double[] lab = xyzToLab(xyz[0], xyz[1], xyz[2]);
        return lab;
    }

    public static int[] labToRgb(double l, double a, double b) {
        double[] xyz = labToXyz(l, a, b);
        int[] rgb = xyzToRgb(xyz[0], xyz[1], xyz[2]);
        return rgb;
    }

    private static double[] rgbToXyz(int r, int g, int b) {
        double rLinear = r / 255.0;
        double gLinear = g / 255.0;
        double bLinear = b / 255.0;

        if (rLinear > 0.04045) {
            rLinear = Math.pow((rLinear + 0.055) / 1.055, 2.4);
        } else {
            rLinear = rLinear / 12.92;
        }

        if (gLinear > 0.04045) {
            gLinear = Math.pow((gLinear + 0.055) / 1.055, 2.4);
        } else {
            gLinear = gLinear / 12.92;
        }

        if (bLinear > 0.04045) {
            bLinear = Math.pow((bLinear + 0.055) / 1.055, 2.4);
        } else {
            bLinear = bLinear / 12.92;
        }

        double x = rLinear * 0.4124 + gLinear * 0.3576 + bLinear * 0.1805;
        double y = rLinear * 0.2126 + gLinear * 0.7152 + bLinear * 0.0722;
        double z = rLinear * 0.0193 + gLinear * 0.1192 + bLinear * 0.9505;

        return new double[]{x, y, z};
    }

    private static int[] xyzToRgb(double x, double y, double z) {
        double rLinear = x * 3.2406 + y * -1.5372 + z * -0.4986;
        double gLinear = x * -0.9689 + y * 1.8758 + z * 0.0415;
        double bLinear = x * 0.0557 + y * -0.2040 + z * 1.0570;

        if (rLinear > 0.0031308) {
            rLinear = 1.055 * Math.pow(rLinear, 1.0 / 2.4) - 0.055;
        } else {
            rLinear = 12.92 * rLinear;
        }

        if (gLinear > 0.0031308) {
            gLinear = 1.055 * Math.pow(gLinear, 1.0 / 2.4) - 0.055;
        } else {
            gLinear = 12.92 * gLinear;
        }

        if (bLinear > 0.0031308) {
            bLinear = 1.055 * Math.pow(bLinear, 1.0 / 2.4) - 0.055;
        } else {
            bLinear = 12.92 * bLinear;
        }

        int r = (int) (rLinear * 255 + 0.5);
        int g = (int) (gLinear * 255 + 0.5);
        int b = (int) (bLinear * 255 + 0.5);

        return new int[]{r, g, b};
    }

    private static double[] xyzToLab(double x, double y, double z) {
        double[] lab = new double[3];

        x = x / REF_X;
        y = y / REF_Y;
        z = z / REF_Z;

        if (x > XYZ_EPSILON) {
            x = Math.pow(x, 1.0 / 3.0);
        } else {
            x = (XYZ_KAPPA * x + 16.0) / 116.0;
        }

        if (y > XYZ_EPSILON) {
            y = Math.pow(y, 1.0 / 3.0);
        } else {
            y = (XYZ_KAPPA * y + 16.0) / 116.0;
        }

        if (z > XYZ_EPSILON) {
            z = Math.pow(z, 1.0 / 3.0);
        } else {
            z = (XYZ_KAPPA * z + 16.0) / 116.0;
        }

        lab[0] = 116.0 * y - 16.0;
        lab[1] = 500.0 * (x - y);
        lab[2] = 200.0 * (y - z);

        return lab;
    }
    private static double[] labToXyz(double l, double a, double b) {
        double[] xyz = new double[3];

        double fy = (l + 16.0) / 116.0;
        double fx = a / 500.0 + fy;
        double fz = fy - b / 200.0;

        double fx3 = fx * fx * fx;
        double fz3 = fz * fz * fz;

        if (fx3 > XYZ_EPSILON) {
            xyz[0] = REF_X * fx3;
        } else {
            xyz[0] = (fx - 16.0 / 116.0) / 7.787 * REF_X;
        }

        if (l > (XYZ_KAPPA * XYZ_EPSILON)) {
            xyz[1] = REF_Y * Math.pow((l + 16.0) / 116.0, 3.0);
        } else {
            xyz[1] = l / XYZ_KAPPA * REF_Y;
        }

        if (fz3 > XYZ_EPSILON) {
            xyz[2] = REF_Z * fz3;
        } else {
            xyz[2] = (fz - 16.0 / 116.0) / 7.787 * REF_Z;
        }

        return xyz;
    }
}