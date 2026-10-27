package com.warpdmgtrainer.model;

public enum EndReason {
    DEATH("Morte"), MANUAL("Encerramento manual"), INTERRUPTED("Interrupção (mundo/conexão fechado)");

    private final String label;
    EndReason(String label) { this.label = label; }
    public String getLabel() { return label; }
}
