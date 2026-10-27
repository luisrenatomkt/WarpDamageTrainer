package com.warpdmgtrainer.model;

/** Níveis de dano por golpe. 2 pontos de vida = 1 coração. */
public enum DamageLevel {
    H1_0(2f, "1"), H1_5(3f, "1,5"), H2_0(4f, "2"), H2_5(5f, "2,5"), H3_0(6f, "3"),
    H3_5(7f, "3,5"), H4_0(8f, "4"), H4_5(9f, "4,5"), H5_0(10f, "5");

    private final float healthPoints;
    private final String label;

    DamageLevel(float healthPoints, String label) {
        this.healthPoints = healthPoints;
        this.label = label;
    }

    public float getHealthPoints() { return healthPoints; }
    public float getHearts() { return healthPoints / 2f; }
    public String getLabel() { return label; }

    /** Ex.: "2,5 corações (5 pts)". */
    public String getDescription() {
        String unit = healthPoints == 2f ? " coração" : " corações";
        return label + unit + " (" + (int) healthPoints + " pts)";
    }

    public static DamageLevel fromIndex(int index) {
        DamageLevel[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, index))];
    }

    /** Retorna o nível correspondente a N corações, ou null. */
    public static DamageLevel fromHearts(double hearts) {
        for (DamageLevel d : values()) {
            if (Math.abs(d.getHearts() - hearts) < 0.001) return d;
        }
        return null;
    }
}
