/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.event.EventHandler
 *  javafx.event.EventType
 *  javafx.scene.Cursor
 *  javafx.scene.canvas.Canvas
 *  javafx.scene.canvas.GraphicsContext
 *  javafx.scene.input.MouseEvent
 *  javafx.scene.paint.Color
 *  javafx.scene.paint.Paint
 */
package net.ybroad.dispy.manager.util;

import java.util.HashMap;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class CanvasDrawer {
    private static Color[] COLORS = new Color[]{Color.rgb((int)25, (int)171, (int)144), Color.rgb((int)155, (int)82, (int)160), Color.rgb((int)255, (int)153, (int)0), Color.rgb((int)59, (int)154, (int)218)};
    private static HashMap<Canvas, CanvasInfo> infos = new HashMap();

    public static void drawCanvas(Canvas canvas, int n, int[] nArray, int n2, int n3) {
        if (nArray == null || n2 == 0 || n3 == 0) {
            GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
            graphicsContext.save();
            graphicsContext.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
            graphicsContext.restore();
            return;
        }
        double d = canvas.getWidth() - 1.0;
        double d2 = canvas.getHeight() - 1.0;
        double d3 = Math.min(d / (double)n2, d2 / (double)n3);
        double d4 = d3 * (double)n2;
        double d5 = d3 * (double)n3;
        double d6 = (d - d4) / 2.0 + 0.5;
        double d7 = (d2 - d5) / 2.0 + 0.5;
        double[] dArray = new double[]{d3 * (double)nArray[0], d3 * (double)nArray[1], d3 * (double)nArray[2], d3 * (double)nArray[3]};
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.save();
        graphicsContext.clearRect(0.0, 0.0, d + 1.0, d2 + 1.0);
        switch (n) {
            case 1: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7 + dArray[0], d4, dArray[1]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6, d7 + dArray[0] + dArray[1], d4, dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6, d7 + dArray[0] + dArray[1] + dArray[2], d4, dArray[3]);
                break;
            }
            case 2: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7 + dArray[0], dArray[1], d5 - dArray[0]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6 + dArray[1], d7 + dArray[0], dArray[2], d5 - dArray[0]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6 + dArray[1] + dArray[2], d7 + dArray[0], dArray[3], d5 - dArray[0]);
                break;
            }
            case 3: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7 + dArray[0], dArray[1], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6 + dArray[1], d7 + dArray[0], d4 - dArray[1], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6, d7 + dArray[0] + dArray[2], d4, dArray[3]);
                break;
            }
            case 4: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7 + dArray[0], dArray[1], dArray[2] + dArray[3]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6 + dArray[1], d7 + dArray[0], d4 - dArray[1], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6 + dArray[1], d7 + dArray[0] + dArray[2], d4 - dArray[1], dArray[3]);
                break;
            }
            case 5: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6 + d4 - dArray[1], d7 + dArray[0], dArray[1], dArray[2] + dArray[3]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6, d7 + dArray[0], d4 - dArray[1], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6, d7 + dArray[0] + dArray[2], d4 - dArray[1], dArray[3]);
                break;
            }
            case 6: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4 - dArray[1], dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6 + d4 - dArray[1], d7, dArray[1], dArray[0]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6, d7 + dArray[0], d4, dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6, d7 + dArray[0] + dArray[2], d4, dArray[3]);
                break;
            }
            case 7: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4 - dArray[1], dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6 + d4 - dArray[1], d7, dArray[1], dArray[0]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6, d7 + dArray[0], d4 - dArray[3], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6 + d4 - dArray[3], d7 + dArray[0], dArray[3], dArray[2]);
                break;
            }
            case 8: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4 - dArray[3], dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7 + dArray[0], d4 - dArray[3], dArray[1]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6, d7 + dArray[0] + dArray[1], d4 - dArray[3], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6 + d4 - dArray[3], d7, dArray[3], dArray[0] + dArray[1] + dArray[2]);
                break;
            }
            case 9: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6 + dArray[3], d7, d4 - dArray[3], dArray[0]);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6 + dArray[3], d7 + dArray[0], d4 - dArray[3], dArray[1]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6 + dArray[3], d7 + dArray[0] + dArray[1], d4 - dArray[3], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6, d7, dArray[3], dArray[0] + dArray[1] + dArray[2]);
                break;
            }
            case 10: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, d5);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7, d4, dArray[1]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6, d7 + dArray[1], d4, dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6, d7 + dArray[1] + dArray[2], d4, dArray[3]);
                break;
            }
            case 11: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, d5);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7, dArray[1], d5);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6 + dArray[1], d7, dArray[2], d5);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6 + dArray[1] + dArray[2], d7, dArray[3], d5);
                break;
            }
            case 12: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, d5);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7, dArray[1], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6 + dArray[1], d7, d4 - dArray[1], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6, d7 + dArray[2], d4, dArray[3]);
                break;
            }
            case 13: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, d5);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7, d4, dArray[1]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6, d7 + dArray[1], dArray[2], dArray[3]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6 + dArray[2], d7 + dArray[1], d4 - dArray[2], dArray[3]);
                break;
            }
            case 14: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, d5);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6 + dArray[3], d7, d4 - dArray[3], dArray[1]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6 + dArray[3], d7 + dArray[1], d4 - dArray[3], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6, d7, dArray[3], dArray[1] + dArray[2]);
                break;
            }
            case 15: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, d4, d5);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6, d7, d4 - dArray[3], dArray[1]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6, d7 + dArray[1], d4 - dArray[3], dArray[2]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6 + d4 - dArray[3], d7, dArray[3], dArray[1] + dArray[2]);
                break;
            }
            case 16: {
                graphicsContext.setFill((Paint)COLORS[0]);
                graphicsContext.fillRect(d6, d7, dArray[0], d5);
                graphicsContext.setFill((Paint)COLORS[1]);
                graphicsContext.fillRect(d6 + dArray[0], d7, dArray[2], dArray[1]);
                graphicsContext.setFill((Paint)COLORS[2]);
                graphicsContext.fillRect(d6 + dArray[0], d7 + dArray[1], dArray[2], d5 - dArray[1]);
                graphicsContext.setFill((Paint)COLORS[3]);
                graphicsContext.fillRect(d6 + dArray[0] + dArray[2], d7, dArray[3], d5);
            }
        }
        graphicsContext.setStroke((Paint)Color.BLACK);
        graphicsContext.setLineWidth(1.0);
        graphicsContext.strokeRect(d6, d7, d4, d5);
        graphicsContext.restore();
        CanvasInfo canvasInfo = infos.get(canvas);
        if (canvasInfo != null) {
            canvasInfo.type = n;
            ((CanvasInfo)canvasInfo).size[0] = nArray[0];
            ((CanvasInfo)canvasInfo).size[1] = nArray[1];
            ((CanvasInfo)canvasInfo).size[2] = nArray[2];
            ((CanvasInfo)canvasInfo).size[3] = nArray[3];
            canvasInfo.devW = n2;
            canvasInfo.devH = n3;
            canvasInfo.scale = d3;
            canvasInfo.scaleW = d4;
            canvasInfo.scaleH = d5;
            canvasInfo.offsetX = d6;
            canvasInfo.offsetY = d7;
            ((CanvasInfo)canvasInfo).scaleSize[0] = dArray[0];
            ((CanvasInfo)canvasInfo).scaleSize[1] = dArray[1];
            ((CanvasInfo)canvasInfo).scaleSize[2] = dArray[2];
            ((CanvasInfo)canvasInfo).scaleSize[3] = dArray[3];
        }
    }

    public static void createListener(Canvas canvas, CavasInfoListener cavasInfoListener) {
        infos.put(canvas, new CanvasInfo(canvas, cavasInfoListener));
    }

    public static void deleteListener(Canvas canvas) {
        infos.remove(canvas);
    }

    public static interface CavasInfoListener {
        public void sizeChanged(int[] var1);
    }

    private static class CanvasInfo
    implements EventHandler<MouseEvent> {
        private Canvas canvas;
        private CavasInfoListener listener;
        private int type = 0;
        private int[] size = new int[4];
        private int devW = 0;
        private int devH = 0;
        private double scale = 0.0;
        private double scaleW = 0.0;
        private double scaleH = 0.0;
        private double offsetX = 0.0;
        private double offsetY = 0.0;
        private double[] scaleSize = new double[4];
        private boolean moving = false;
        private int movingIndex = -1;
        private final double MOVING = 3.0;

        private CanvasInfo(Canvas canvas, CavasInfoListener cavasInfoListener) {
            this.canvas = canvas;
            this.listener = cavasInfoListener;
            canvas.setOnMouseMoved((EventHandler)this);
            canvas.setOnMouseDragged((EventHandler)this);
            canvas.setOnMousePressed((EventHandler)this);
            canvas.setOnMouseReleased((EventHandler)this);
        }

        public void handle(MouseEvent mouseEvent) {
            EventType eventType = mouseEvent.getEventType();
            if (eventType == MouseEvent.MOUSE_PRESSED) {
                this.moving = true;
            } else if (eventType == MouseEvent.MOUSE_RELEASED) {
                this.moving = false;
                this.movingIndex = -1;
            } else if (eventType == MouseEvent.MOUSE_MOVED) {
                this.canvas.setCursor(Cursor.DEFAULT);
                double d = mouseEvent.getX();
                double d2 = mouseEvent.getY();
                switch (this.type) {
                    case 1: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[1] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[1] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetX < d) || !(d < this.offsetX + this.scaleW) || !(Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[1] + this.scaleSize[2] + this.scaleSize[3] - d2) < 3.0)) break;
                        this.canvas.setCursor(Cursor.V_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 2: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetY + this.scaleSize[0] < d2 && d2 < this.offsetY + this.scaleH && Math.abs(this.offsetX + this.scaleSize[1] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetY + this.scaleSize[0] < d2 && d2 < this.offsetY + this.scaleH && Math.abs(this.offsetX + this.scaleSize[1] + this.scaleSize[2] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetY + this.scaleSize[0] < d2) || !(d2 < this.offsetY + this.scaleH) || !(Math.abs(this.offsetX + this.scaleSize[1] + this.scaleSize[2] + this.scaleSize[3] - d) < 3.0)) break;
                        this.canvas.setCursor(Cursor.H_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 3: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetY + this.scaleSize[0] < d2 && d2 < this.offsetY + this.scaleSize[0] + this.scaleSize[2] && Math.abs(this.offsetX + this.scaleSize[1] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetX < d) || !(d < this.offsetX + this.scaleW) || !(Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] + this.scaleSize[3] - d2) < 3.0)) break;
                        this.canvas.setCursor(Cursor.V_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 4: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetY + this.scaleSize[0] < d2 && d2 < this.offsetY + this.scaleSize[0] + this.scaleSize[2] + this.scaleSize[3] && Math.abs(this.offsetX + this.scaleSize[1] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX + this.scaleSize[1] < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetX < d) || !(d < this.offsetX + this.scaleW) || !(Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] + this.scaleSize[3] - d2) < 3.0)) break;
                        this.canvas.setCursor(Cursor.V_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 5: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetY + this.scaleSize[0] < d2 && d2 < this.offsetY + this.scaleSize[0] + this.scaleSize[2] + this.scaleSize[3] && Math.abs(this.offsetX + this.scaleW - this.scaleSize[1] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW - this.scaleSize[1] && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetX < d) || !(d < this.offsetX + this.scaleW) || !(Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] + this.scaleSize[3] - d2) < 3.0)) break;
                        this.canvas.setCursor(Cursor.V_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 6: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetY < d2 && d2 < this.offsetY + this.scaleSize[0] && Math.abs(this.offsetX + this.scaleW - this.scaleSize[1] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetX < d) || !(d < this.offsetX + this.scaleW) || !(Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] + this.scaleSize[3] - d2) < 3.0)) break;
                        this.canvas.setCursor(Cursor.V_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 7: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetY < d2 && d2 < this.offsetY + this.scaleSize[0] && Math.abs(this.offsetX + this.scaleW - this.scaleSize[1] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetY + this.scaleSize[0] < d2) || !(d2 < this.offsetY + this.scaleSize[0] + this.scaleSize[2]) || !(Math.abs(this.offsetX + this.scaleW - this.scaleSize[3] - d) < 3.0)) break;
                        this.canvas.setCursor(Cursor.H_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 8: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW - this.scaleSize[3] && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW - this.scaleSize[3] && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[1] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[1] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetY < d2) || !(d2 < this.offsetY + this.scaleSize[0] + this.scaleSize[1] + this.scaleSize[2]) || !(Math.abs(this.offsetX + this.scaleW - this.scaleSize[3] - d) < 3.0)) break;
                        this.canvas.setCursor(Cursor.H_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 9: {
                        if (this.offsetX + this.scaleSize[3] < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetX + this.scaleSize[3] < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[1] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[0] + this.scaleSize[1] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetY < d2) || !(d2 < this.offsetY + this.scaleSize[0] + this.scaleSize[1] + this.scaleSize[2]) || !(Math.abs(this.offsetX + this.scaleSize[3] - d) < 3.0)) break;
                        this.canvas.setCursor(Cursor.H_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 10: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[1] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[1] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetX < d) || !(d < this.offsetX + this.scaleW) || !(Math.abs(this.offsetY + this.scaleSize[1] + this.scaleSize[2] + this.scaleSize[3] - d2) < 3.0)) break;
                        this.canvas.setCursor(Cursor.V_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 11: {
                        if (this.offsetY < d2 && d2 < this.offsetY + this.scaleH && Math.abs(this.offsetX + this.scaleSize[1] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetY < d2 && d2 < this.offsetY + this.scaleH && Math.abs(this.offsetX + this.scaleSize[1] + this.scaleSize[2] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetY < d2) || !(d2 < this.offsetY + this.scaleH) || !(Math.abs(this.offsetX + this.scaleSize[1] + this.scaleSize[2] + this.scaleSize[3] - d) < 3.0)) break;
                        this.canvas.setCursor(Cursor.H_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 12: {
                        if (this.offsetY < d2 && d2 < this.offsetY + this.scaleSize[2] && Math.abs(this.offsetX + this.scaleSize[1] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetX < d) || !(d < this.offsetX + this.scaleW) || !(Math.abs(this.offsetY + this.scaleSize[2] + this.scaleSize[3] - d2) < 3.0)) break;
                        this.canvas.setCursor(Cursor.V_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 13: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[1] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetY + this.scaleSize[1] < d2 && d2 < this.offsetY + this.scaleSize[1] + this.scaleSize[3] && Math.abs(this.offsetX + this.scaleSize[2] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetX < d) || !(d < this.offsetX + this.scaleW) || !(Math.abs(this.offsetY + this.scaleSize[1] + this.scaleSize[3] - d2) < 3.0)) break;
                        this.canvas.setCursor(Cursor.V_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 14: {
                        if (this.offsetX + this.scaleSize[3] < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[1] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[1] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetY < d2) || !(d2 < this.offsetY + this.scaleSize[1] + this.scaleSize[2]) || !(Math.abs(this.offsetX + this.scaleSize[3] - d) < 3.0)) break;
                        this.canvas.setCursor(Cursor.H_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 15: {
                        if (this.offsetX < d && d < this.offsetX + this.scaleW - this.scaleSize[3] && Math.abs(this.offsetY - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW - this.scaleSize[3] && Math.abs(this.offsetY + this.scaleSize[1] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetX < d && d < this.offsetX + this.scaleW && Math.abs(this.offsetY + this.scaleSize[1] + this.scaleSize[2] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetY < d2) || !(d2 < this.offsetY + this.scaleSize[1] + this.scaleSize[2]) || !(Math.abs(this.offsetX + this.scaleW - this.scaleSize[3] - d) < 3.0)) break;
                        this.canvas.setCursor(Cursor.H_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                    case 16: {
                        if (this.offsetY < d2 && d2 < this.offsetY + this.scaleH && Math.abs(this.offsetX + this.scaleSize[0] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 0;
                            break;
                        }
                        if (this.offsetX + this.scaleSize[0] < d && d < this.offsetX + this.scaleSize[0] + this.scaleSize[2] && Math.abs(this.offsetY + this.scaleSize[1] - d2) < 3.0) {
                            this.canvas.setCursor(Cursor.V_RESIZE);
                            this.movingIndex = 1;
                            break;
                        }
                        if (this.offsetY < d2 && d2 < this.offsetY + this.scaleH && Math.abs(this.offsetX + this.scaleSize[0] + this.scaleSize[2] - d) < 3.0) {
                            this.canvas.setCursor(Cursor.H_RESIZE);
                            this.movingIndex = 2;
                            break;
                        }
                        if (!(this.offsetY < d2) || !(d2 < this.offsetY + this.scaleH) || !(Math.abs(this.offsetX + this.scaleSize[0] + this.scaleSize[2] + this.scaleSize[3] - d) < 3.0)) break;
                        this.canvas.setCursor(Cursor.H_RESIZE);
                        this.movingIndex = 3;
                        break;
                    }
                }
            } else if (eventType == MouseEvent.MOUSE_DRAGGED && this.moving && this.movingIndex >= 0) {
                int n = (int)((mouseEvent.getX() - this.offsetX) / this.scale + 0.5);
                int n2 = (int)((mouseEvent.getY() - this.offsetY) / this.scale + 0.5);
                block18 : switch (this.type) {
                    case 1: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[0] + this.size[1]));
                                this.size[1] = this.size[0] + this.size[1] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                n2 = Math.max(this.size[0], Math.min(n2, this.size[0] + this.size[1] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[1] + this.size[2] - n2;
                                this.size[1] = n2 - this.size[0];
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[0] + this.size[1], Math.min(n2, this.size[0] + this.size[1] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[0] + this.size[1] + this.size[2] + this.size[3] - n2;
                                this.size[2] = n2 - this.size[0] - this.size[1];
                                break block18;
                            }
                            case 3: {
                                n2 = Math.max(this.size[0] + this.size[1] + this.size[2], Math.min(n2, this.devH));
                                this.size[3] = n2 - this.size[0] - this.size[1] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 2: {
                        switch (this.movingIndex) {
                            case 0: {
                                this.size[0] = n2 = Math.max(0, Math.min(n2, this.devH));
                                break block18;
                            }
                            case 1: {
                                n = Math.max(0, Math.min(n, this.size[1] + this.size[2]));
                                this.size[2] = this.size[1] + this.size[2] - n;
                                this.size[1] = n;
                                break block18;
                            }
                            case 2: {
                                n = Math.max(this.size[1], Math.min(n, this.size[1] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[1] + this.size[2] + this.size[3] - n;
                                this.size[2] = n - this.size[1];
                                break block18;
                            }
                            case 3: {
                                n = Math.max(this.size[1] + this.size[2], Math.min(n, this.devW));
                                this.size[3] = n - this.size[1] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 3: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[0] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[2] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                this.size[1] = n = Math.max(0, Math.min(n, this.devW));
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[0], Math.min(n2, this.size[0] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[0] + this.size[2] + this.size[3] - n2;
                                this.size[2] = n2 - this.size[0];
                                break block18;
                            }
                            case 3: {
                                n2 = Math.max(this.size[0] + this.size[2], Math.min(n2, this.devH));
                                this.size[3] = n2 - this.size[0] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 4: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[0] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[2] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                this.size[1] = n = Math.max(0, Math.min(n, this.devW));
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[0], Math.min(n2, this.size[0] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[0] + this.size[2] + this.size[3] - n2;
                                this.size[2] = n2 - this.size[0];
                                break block18;
                            }
                            case 3: {
                                n2 = Math.max(this.size[0] + this.size[2], Math.min(n2, this.devH));
                                this.size[3] = n2 - this.size[0] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 5: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[0] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[2] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                n = Math.max(0, Math.min(n, this.devW));
                                this.size[1] = this.devW - n;
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[0], Math.min(n2, this.size[0] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[0] + this.size[2] + this.size[3] - n2;
                                this.size[2] = n2 - this.size[0];
                                break block18;
                            }
                            case 3: {
                                n2 = Math.max(this.size[0] + this.size[2], Math.min(n2, this.devH));
                                this.size[3] = n2 - this.size[0] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 6: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[0] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[2] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                n = Math.max(0, Math.min(n, this.devW));
                                this.size[1] = this.devW - n;
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[0], Math.min(n2, this.size[0] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[0] + this.size[2] + this.size[3] - n2;
                                this.size[2] = n2 - this.size[0];
                                break block18;
                            }
                            case 3: {
                                n2 = Math.max(this.size[0] + this.size[2], Math.min(n2, this.devH));
                                this.size[3] = n2 - this.size[0] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 7: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[0] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[2] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                n = Math.max(0, Math.min(n, this.devW));
                                this.size[1] = this.devW - n;
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[0], Math.min(n2, this.devH));
                                this.size[2] = n2 - this.size[0];
                                break block18;
                            }
                            case 3: {
                                n = Math.max(0, Math.min(n, this.devW));
                                this.size[3] = this.devW - n;
                                break block18;
                            }
                        }
                        break;
                    }
                    case 8: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[0] + this.size[1]));
                                this.size[1] = this.size[0] + this.size[1] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                n2 = Math.max(this.size[0], Math.min(n2, this.size[0] + this.size[1] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[1] + this.size[2] - n2;
                                this.size[1] = n2 - this.size[0];
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[0] + this.size[1], Math.min(n2, this.devH));
                                this.size[2] = n2 - this.size[0] - this.size[1];
                                break block18;
                            }
                            case 3: {
                                n = Math.max(0, Math.min(n, this.devW));
                                this.size[3] = this.devW - n;
                                break block18;
                            }
                        }
                        break;
                    }
                    case 9: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[0] + this.size[1]));
                                this.size[1] = this.size[0] + this.size[1] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                n2 = Math.max(this.size[0], Math.min(n2, this.size[0] + this.size[1] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[1] + this.size[2] - n2;
                                this.size[1] = n2 - this.size[0];
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[0] + this.size[1], Math.min(n2, this.devH));
                                this.size[2] = n2 - this.size[0] - this.size[1];
                                break block18;
                            }
                            case 3: {
                                this.size[3] = n = Math.max(0, Math.min(n, this.devW));
                                break block18;
                            }
                        }
                        break;
                    }
                    case 10: {
                        switch (this.movingIndex) {
                            case 1: {
                                n2 = Math.max(0, Math.min(n2, this.size[1] + this.size[2]));
                                this.size[2] = this.size[1] + this.size[2] - n2;
                                this.size[1] = n2;
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[1], Math.min(n2, this.size[1] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[1] + this.size[2] + this.size[3] - n2;
                                this.size[2] = n2 - this.size[1];
                                break block18;
                            }
                            case 3: {
                                n2 = Math.max(this.size[1] + this.size[2], Math.min(n2, this.devH));
                                this.size[3] = n2 - this.size[1] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 11: {
                        switch (this.movingIndex) {
                            case 1: {
                                n = Math.max(0, Math.min(n, this.size[1] + this.size[2]));
                                this.size[2] = this.size[1] + this.size[2] - n;
                                this.size[1] = n;
                                break block18;
                            }
                            case 2: {
                                n = Math.max(this.size[1], Math.min(n, this.size[1] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[1] + this.size[2] + this.size[3] - n;
                                this.size[2] = n - this.size[1];
                                break block18;
                            }
                            case 3: {
                                n = Math.max(this.size[1] + this.size[2], Math.min(n, this.devW));
                                this.size[3] = n - this.size[1] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 12: {
                        switch (this.movingIndex) {
                            case 1: {
                                this.size[1] = n = Math.max(0, Math.min(n, this.devW));
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(0, Math.min(n2, this.size[2] + this.size[3]));
                                this.size[3] = this.size[2] + this.size[3] - n2;
                                this.size[2] = n2;
                                break block18;
                            }
                            case 3: {
                                n2 = Math.max(this.size[2], Math.min(n2, this.devH));
                                this.size[3] = n2 - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 13: {
                        switch (this.movingIndex) {
                            case 1: {
                                n2 = Math.max(0, Math.min(n2, this.size[1] + this.size[3]));
                                this.size[3] = this.size[1] + this.size[3] - n2;
                                this.size[1] = n2;
                                break block18;
                            }
                            case 2: {
                                this.size[2] = n = Math.max(0, Math.min(n, this.devW));
                                break block18;
                            }
                            case 3: {
                                n2 = Math.max(this.size[1], Math.min(n2, this.devH));
                                this.size[3] = n2 - this.size[1];
                                break block18;
                            }
                        }
                        break;
                    }
                    case 14: {
                        switch (this.movingIndex) {
                            case 1: {
                                n2 = Math.max(0, Math.min(n2, this.size[1] + this.size[2]));
                                this.size[2] = this.size[1] + this.size[2] - n2;
                                this.size[1] = n2;
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[1], Math.min(n2, this.devH));
                                this.size[2] = n2 - this.size[1];
                                break block18;
                            }
                            case 3: {
                                this.size[3] = n = Math.max(0, Math.min(n, this.devW));
                                break block18;
                            }
                        }
                        break;
                    }
                    case 15: {
                        switch (this.movingIndex) {
                            case 0: {
                                n2 = Math.max(0, Math.min(n2, this.size[1]));
                                this.size[1] = this.size[0] + this.size[1] - n2;
                                this.size[0] = n2;
                                break block18;
                            }
                            case 1: {
                                n2 = Math.max(0, Math.min(n2, this.size[1] + this.size[2]));
                                this.size[2] = this.size[1] + this.size[2] - n2;
                                this.size[1] = n2;
                                break block18;
                            }
                            case 2: {
                                n2 = Math.max(this.size[1], Math.min(n2, this.devH));
                                this.size[2] = n2 - this.size[1];
                                break block18;
                            }
                            case 3: {
                                n = Math.max(0, Math.min(n, this.devW));
                                this.size[3] = this.devW - n;
                                break block18;
                            }
                        }
                        break;
                    }
                    case 16: {
                        switch (this.movingIndex) {
                            case 0: {
                                n = Math.max(0, Math.min(n, this.size[0] + this.size[2]));
                                this.size[2] = this.size[0] + this.size[2] - n;
                                this.size[0] = n;
                                break block18;
                            }
                            case 1: {
                                this.size[1] = n2 = Math.max(0, Math.min(n2, this.devH));
                                break block18;
                            }
                            case 2: {
                                n = Math.max(this.size[0], Math.min(n, this.size[0] + this.size[2] + this.size[3]));
                                this.size[3] = this.size[0] + this.size[2] + this.size[3] - n;
                                this.size[2] = n - this.size[0];
                                break block18;
                            }
                            case 3: {
                                n = Math.max(this.size[0] + this.size[2], Math.min(n, this.devW));
                                this.size[3] = n - this.size[0] - this.size[2];
                                break block18;
                            }
                        }
                        break;
                    }
                }
                this.listener.sizeChanged(this.size);
            }
        }
    }
}

