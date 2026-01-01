package com.fruitsicecream.fruitsicecreamutilities.util;

/**
 * Tracker simple para detectar cuando el mundo se está guardando
 */
public class SaveStateTracker {
    private static volatile boolean isSaving = false;

    public static void markSaving() {
        isSaving = true;
    }

    public static void markSaved() {
        isSaving = false;
    }

    public static boolean isSaving() {
        return isSaving;
    }
}