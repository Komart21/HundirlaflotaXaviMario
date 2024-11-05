package com.project;

public class PlayGrid {

    private final double startX;
    private final double startY;
    private final double cellSize;
    private final int cols;
    private final int rows;

    public PlayGrid(double startX, double startY, double cellSize, int rows, int cols) {
        this.startX = startX;
        this.startY = startY;
        this.cellSize = cellSize;
        this.cols = cols;
        this.rows = rows;
    }

    public boolean isPositionInsideGrid(double x, double y) {
        return x >= startX && x < startX + cols * cellSize &&
               y >= startY && y < startY + rows * cellSize;
    }  

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getCellSize() {
        return cellSize;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    // New methods to calculate grid width and height
    public double getWidth() {
        return cols * cellSize; // Total width of the grid
    }

    public double getHeight() {
        return rows * cellSize; // Total height of the grid
    }

    public int getCol(double x) {
        if (x < startX || x >= startX + cols * cellSize) {
            return -1; // Out of grid bounds
        }
        return (int) ((x - startX) / cellSize);
    }

    public int getRow(double y) {
        if (y < startY || y >= startY + rows * cellSize) {
            return -1; // Out of grid bounds
        }
        return (int) ((y - startY) / cellSize);
    }

    public double getCellX(int col) {
        return (startX + col * cellSize);
    }

    public double getCellY(int row) {
        return (startY + row * cellSize);
    }
}

