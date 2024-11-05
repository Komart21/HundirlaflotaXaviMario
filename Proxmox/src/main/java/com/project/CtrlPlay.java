package com.project;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CtrlPlay implements Initializable {

    @FXML
    private Canvas canvas;
    private GraphicsContext gc;
    private Boolean showFPS = false;

    private PlayTimer animationTimer;
    private PlayGrid grid;

    public Map<String, JSONObject> clientMousePositions = new HashMap<>();
    private Boolean mouseDragging = false;
    private double mouseOffsetX, mouseOffsetY;

    public static Map<String, JSONObject> selectableObjects = new HashMap<>();
    private String selectedObject = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Get drawing context
        this.gc = canvas.getGraphicsContext2D();

        // Set listeners
        UtilsViews.parentContainer.heightProperty().addListener((observable, oldValue, newvalue) -> { onSizeChanged(); });
        UtilsViews.parentContainer.widthProperty().addListener((observable, oldValue, newvalue) -> { onSizeChanged(); });
        
        canvas.setOnMouseMoved(this::setOnMouseMoved);
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);

        // Define grid
        grid = new PlayGrid(25, 25, 25, 9, 9); // Cambia el número de filas y columnas si es necesario

        // Initialize boats (barcos) in visible positions
        initializeBoats();

        // Start run/draw timer bucle
        animationTimer = new PlayTimer(this::run, this::draw, 60); // Set FPS to 60 or whatever is appropriate
        start();
    }

    private void initializeBoats() {
        // Crear barcos en posiciones visibles dentro de la tabla
        addNewBoat("boat1", 3, 1); // Barco de 3 columnas y 1 fila
        selectableObjects.get("boat1").put("x", 50); // Posición visible en la cuadrícula
        selectableObjects.get("boat1").put("y", 50);
        
        addNewBoat("boat2", 4, 1); // Barco de 4 columnas y 1 fila
        selectableObjects.get("boat2").put("x", 150); // Posición visible en la cuadrícula
        selectableObjects.get("boat2").put("y", 50);
        
        addNewBoat("boat3", 2, 2); // Barco de 2 columnas y 2 filas
        selectableObjects.get("boat3").put("x", 100); // Posición visible en la cuadrícula
        selectableObjects.get("boat3").put("y", 150);
    }

    public void addNewBoat(String id, int cols, int rows) {
        JSONObject newBoat = new JSONObject();
        newBoat.put("objectId", id);
        newBoat.put("x", 0); // Inicializa en posición X deseada
        newBoat.put("y", 0); // Inicializa en posición Y deseada
        newBoat.put("cols", cols);
        newBoat.put("rows", rows);
        
        selectableObjects.put(id, newBoat);
    }

    // When window changes its size
    public void onSizeChanged() {
        double width = UtilsViews.parentContainer.getWidth();
        double height = UtilsViews.parentContainer.getHeight();
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    // Start animation timer
    public void start() {
        animationTimer.start();
    }

    // Stop animation timer
    public void stop() {
        animationTimer.stop();
    }

    private void setOnMouseMoved(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();

        JSONObject newPosition = new JSONObject();
        newPosition.put("x", mouseX);
        newPosition.put("y", mouseY);
        if (grid.isPositionInsideGrid(mouseX, mouseY)) {                
            newPosition.put("col", grid.getCol(mouseX));
            newPosition.put("row", grid.getRow(mouseY));
        } else {
            newPosition.put("col", -1);
            newPosition.put("row", -1);
        }
        clientMousePositions.put(ClientFX.clientId, newPosition);

        JSONObject msgObj = clientMousePositions.get(ClientFX.clientId);
        msgObj.put("type", "clientMouseMoving");
        msgObj.put("clientId", ClientFX.clientId);
    
        if (ClientFX.wsClient != null) {
            ClientFX.wsClient.safeSend(msgObj.toString());
        }
    }

    private void onMousePressed(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();

        selectedObject = "";
        mouseDragging = false;

        for (String objectId : selectableObjects.keySet()) {
            JSONObject obj = selectableObjects.get(objectId);
            int objX = obj.getInt("x");
            int objY = obj.getInt("y");
            int cols = obj.getInt("cols");
            int rows = obj.getInt("rows");

            if (isPositionInsideObject(mouseX, mouseY, objX, objY,  cols, rows)) {
                selectedObject = objectId;
                mouseDragging = true;
                mouseOffsetX = event.getX() - objX;
                mouseOffsetY = event.getY() - objY;
                break;
            }
        }
    }

    private void onMouseDragged(MouseEvent event) {
        if (mouseDragging) {
            JSONObject obj = selectableObjects.get(selectedObject);
            double objX = event.getX() - mouseOffsetX;
            double objY = event.getY() - mouseOffsetY;
            
            obj.put("x", objX);
            obj.put("y", objY);
            obj.put("col", grid.getCol(objX));
            obj.put("row", grid.getRow(objY));

            JSONObject msgObj = selectableObjects.get(selectedObject);
            msgObj.put("type", "clientSelectableObjectMoving");
            msgObj.put("objectId", obj.getString("objectId"));
        
            if (ClientFX.wsClient != null) {
                ClientFX.wsClient.safeSend(msgObj.toString());
            }
        }
        setOnMouseMoved(event);
    }

    private void onMouseReleased(MouseEvent event) {
        if (!selectedObject.isEmpty()) {
            JSONObject obj = selectableObjects.get(selectedObject);
            int objCol = obj.getInt("col");
            int objRow = obj.getInt("row");

            if (objCol != -1 && objRow != -1) {
                obj.put("x", grid.getCellX(objCol));
                obj.put("y", grid.getCellY(objRow));
            }

            JSONObject msgObj = selectableObjects.get(selectedObject);
            msgObj.put("type", "clientSelectableObjectMoving");
            msgObj.put("objectId", obj.getString("objectId"));
        
            if (ClientFX.wsClient != null) {
                ClientFX.wsClient.safeSend(msgObj.toString());
            }

            mouseDragging = false;
            selectedObject = "";
        }
    }

    public void setPlayersMousePositions(JSONObject positions) {
        clientMousePositions.clear();
        for (String clientId : positions.keySet()) {
            JSONObject positionObject = positions.getJSONObject(clientId);
            clientMousePositions.put(clientId, positionObject);
        }
    }

    public void setSelectableObjects(JSONObject objects) {
        selectableObjects.clear();
        for (String objectId : objects.keySet()) {
            JSONObject positionObject = objects.getJSONObject(objectId);
            selectableObjects.put(objectId, positionObject);
        }
    }

    public Boolean isPositionInsideObject(double positionX, double positionY, int objX, int objY, int cols, int rows) {
        double cellSize = grid.getCellSize();
        double objectWidth = cols * cellSize;
        double objectHeight = rows * cellSize;

        double objectLeftX = objX;
        double objectRightX = objX + objectWidth;
        double objectTopY = objY;
        double objectBottomY = objY + objectHeight;

        return positionX >= objectLeftX && positionX < objectRightX &&
               positionY >= objectTopY && positionY < objectBottomY;
    }

    // Run game (and animations)
    private void run(double fps) {
        if (animationTimer.fps < 1) { return; }

        // Update objects and animations here
    }

    // Draw game to canvas
    public void draw() {
        // Clean drawing area
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw grid
        drawGrid();

        // Draw selectable objects
        for (String objectId : selectableObjects.keySet()) {
            JSONObject obj = selectableObjects.get(objectId);
            double x = obj.getDouble("x");
            double y = obj.getDouble("y");
            double width = obj.getInt("cols") * grid.getCellSize();
            double height = obj.getInt("rows") * grid.getCellSize();
            gc.setFill(Color.BLUE);
            gc.fillRect(x, y, width, height);
        }

        // Draw mouse positions
        for (String clientId : clientMousePositions.keySet()) {
            JSONObject pos = clientMousePositions.get(clientId);
            double mouseX = pos.getDouble("x");
            double mouseY = pos.getDouble("y");

            // Check if the clientId is the current client's ID
            if (clientId.equals(ClientFX.clientId)) {
                gc.setFill(Color.RED); // Current client's mouse is red
            } else {
                gc.setFill(Color.GREEN); // Other clients' mice are green
            }

            if (pos.getInt("col") != -1 && pos.getInt("row") != -1) {
                gc.fillOval(mouseX - 5, mouseY - 5, 10, 10);
            }
        }
    }

    // Draw grid with row and column labels
    private void drawGrid() {
        // Dibuja las líneas de la cuadrícula
        for (int row = 0; row < grid.getRows(); row++) {
            for (int col = 0; col < grid.getCols(); col++) {
                double x = grid.getCellX(col);
                double y = grid.getCellY(row);
                gc.setStroke(Color.BLACK);
                gc.strokeRect(x, y, grid.getCellSize(), grid.getCellSize());
            }
        }

        // Dibuja etiquetas de filas (1 a 9)
        gc.setFill(Color.BLACK);
        gc.setFont(new Font(12));
        for (int i = 0; i < grid.getRows(); i++) {
            gc.fillText(String.valueOf(i + 1), 10, grid.getCellY(i) + grid.getCellSize() / 2);
        }

        // Dibuja etiquetas de columnas (A a H)
        for (int j = 0; j < grid.getCols(); j++) {
            char columnLabel = (char) ('A' + j);
            gc.fillText(String.valueOf(columnLabel), grid.getCellX(j) + grid.getCellSize() / 2, 15);
        }
    }


    public void handleReadyButton(ActionEvent actionEvent) {

    }
}
