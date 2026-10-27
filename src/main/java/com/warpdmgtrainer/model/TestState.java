package com.warpdmgtrainer.model;

public enum TestState {
    IDLE("PARADO"), RUNNING("EM ANDAMENTO"), PAUSED("PAUSADO"), FINISHED("FINALIZADO");

    private final String label;
    TestState(String label) { this.label = label; }
    public String getLabel() { return label; }
}
