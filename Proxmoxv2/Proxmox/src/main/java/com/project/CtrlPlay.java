package com.project;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

    @FXML
    private Button btnReady; // Botón "Listo"

    @FXML
    private Label statusLabel;

    private boolean player0Ready = false;
    private boolean player1Ready = false;

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
        // Barcos en posición horizontal
        addNewBoat("boat1", 3, 1); // Barco de 3 columnas y 1 fila (horizontal)
        selectableObjects.get("boat1").put("x", grid.getCellX(2)); // Colocamos el barco horizontal en la columna C
        selectableObjects.get("boat1").put("y", grid.getCellY(2)); // Fila 3
    
        addNewBoat("boat3", 2, 1); // Barco de 2 columnas y 1 fila (horizontal)
        selectableObjects.get("boat3").put("x", grid.getCellX(4)); // Colocamos el barco en la columna E
        selectableObjects.get("boat3").put("y", grid.getCellY(4)); // Fila 3
    
        // Barcos en posición vertical
        addNewBoat("boat2", 1, 3); // Barco de 1 columna y 3 filas (vertical)
        selectableObjects.get("boat2").put("x", grid.getCellX(6)); // Colocamos el barco vertical en la columna G
        selectableObjects.get("boat2").put("y", grid.getCellY(1)); // Fila 2
    
        addNewBoat("boat4", 1, 2); // Barco de 1 columna y 2 filas (vertical)
        selectableObjects.get("boat4").put("x", grid.getCellX(8)); // Columna I
        selectableObjects.get("boat4").put("y", grid.getCellY(2)); // Fila 3
    }
    
    public void addNewBoat(String id, int cols, int rows) {
        // Crear un nuevo barco
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
            double objCol = obj.getInt("col");
            double objRow = obj.getInt("row");
    
            // Calcular la nueva posición
            double newX = grid.getCellX((int) objCol);
            double newY = grid.getCellY((int) objRow);
    
            // Verificar si la nueva posición causa una colisión
            if (!isCollision(newX, newY, obj.getInt("cols"), obj.getInt("rows"))) {
                // Si no hay colisión, mover el barco
                obj.put("x", newX);
                obj.put("y", newY);
            } else {
                // Si hay colisión, devolver a la posición anterior
                obj.put("x", obj.getDouble("x")); // O almacena la posición original antes de mover
                obj.put("y", obj.getDouble("y"));
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
    
    private boolean isCollision(double x, double y, int cols, int rows) {
        double cellSize = grid.getCellSize();
        double objectWidth = cols * cellSize;
        double objectHeight = rows * cellSize;
    
        double objectLeftX = x;
        double objectRightX = x + objectWidth;
        double objectTopY = y;
        double objectBottomY = y + objectHeight;
    
        // Verificar colisión con otros objetos
        for (String objectId : selectableObjects.keySet()) {
            if (!objectId.equals(selectedObject)) {
                JSONObject otherObj = selectableObjects.get(objectId);
                double otherX = otherObj.getDouble("x");
                double otherY = otherObj.getDouble("y");
                int otherCols = otherObj.getInt("cols");
                int otherRows = otherObj.getInt("rows");
    
                double otherWidth = otherCols * cellSize;
                double otherHeight = otherRows * cellSize;
    
                double otherLeftX = otherX;
                double otherRightX = otherX + otherWidth;
                double otherTopY = otherY;
                double otherBottomY = otherY + otherHeight;
    
                // Comprobar si hay colisión
                if (objectRightX > otherLeftX && objectLeftX < otherRightX &&
                    objectBottomY > otherTopY && objectTopY < otherBottomY) {
                    return true; // Colisión detectada
                }
            }
        }
        return false; // Sin colisión
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
        if (areAllPiecesPlaced()) {

            if (!player0Ready) {
                player0Ready = true;
                statusLabel.setText("Jugador 0 está listo.");
            } else {
                player1Ready = true;
                statusLabel.setText("Jugador 1 está listo.");
            }

            // Verificar si ambos jugadores están listos
            if (player0Ready && player1Ready) {
                startGame();
            }
        } else {
            statusLabel.setText("Por favor, coloca todas las piezas antes de estar listo.");
        }
    }

    private boolean areAllPiecesPlaced() {
        // Check if each boat has a valid position (within the grid)
        for (String boatId : selectableObjects.keySet()) {
            JSONObject boat = selectableObjects.get(boatId);
            double boatX = boat.getDouble("x");
            double boatY = boat.getDouble("y");
            int cols = boat.getInt("cols");
            int rows = boat.getInt("rows");

            if (boatX < 0 || boatY < 0 || boatX >= grid.getWidth() || boatY >= grid.getHeight()) {
                return false; // Boat is not within valid grid bounds
            }
        }
        return true; // All boats are placed correctly
    }

    private void startGame() {
        statusLabel.setText("¡La partida ha comenzado!");
        // Implement game start logic
        // e.g., start a timer or transition to a different game state
    }
}
