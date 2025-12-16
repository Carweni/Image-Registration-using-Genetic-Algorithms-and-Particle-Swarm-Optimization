import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// Classe principal para orquestrar e comparar os algoritmos de registro.
public class ImageRegistration {
    private static class RegistrationResult {
        String name;
        String fitnessType; 
        double fitness;
        long timeMillis;
        double[][] transformationMatrix;
        
        public String getFormattedTime() {
            return String.format("%.2f s", timeMillis / 1000.0);
        }
        public String getFormattedFitness() {
            return String.format("%.4f", fitness);
        }
    }

    public static void main(String[] args) {
        String modelPath = "images/fixed.png";
        String scenePath = "images/moving.png";

        BufferedImage modelImage = ImageTransforms.loadImage(modelPath);
        BufferedImage sceneImage = ImageTransforms.loadImage(scenePath);

        if (modelImage == null || sceneImage == null) {
            System.err.println("Não foi possível carregar as imagens");
            return;
        }
        // Execução dos modelos:
        RegistrationResult[] results = new RegistrationResult[4];

        results[0] = executeGA_MSE(modelImage, sceneImage);        
        results[1] = executeGA_MI(modelImage, sceneImage);
        results[2] = executePSO_MSE(modelImage, sceneImage);
        results[3] = executePSO_MI(modelImage, sceneImage);
        
        // Exibe parâmetros no terminal:
        printFinalResults(results);

        SwingUtilities.invokeLater(() -> showComparisonWindow(modelImage, sceneImage, results));
    }

    private static RegistrationResult executeGA_MSE(BufferedImage model, BufferedImage scene) {
        long startTime = System.currentTimeMillis();
        GeneticMSE ga = new GeneticMSE(model, scene);
        GeneticMSE.Individual bestSolution = ga.runGA();
        long endTime = System.currentTimeMillis();
        
        if (bestSolution == null) return null;

        RegistrationResult result = new RegistrationResult();
        result.name = "GA/MSE";
        result.fitnessType = "MSE";
        result.timeMillis = endTime - startTime;
        result.fitness = bestSolution.getFitness();
        result.transformationMatrix = buildMatrix(bestSolution.getParameters());
        return result;
    }
    
    private static RegistrationResult executeGA_MI(BufferedImage model, BufferedImage scene) {
        long startTime = System.currentTimeMillis();
        GeneticMI ga = new GeneticMI(model, scene);
        GeneticMI.Individual bestSolution = ga.runGA();
        long endTime = System.currentTimeMillis();
        
        if (bestSolution == null) return null;

        RegistrationResult result = new RegistrationResult();
        result.name = "GA/MI";
        result.fitnessType = "MI";
        result.timeMillis = endTime - startTime;
        result.fitness = bestSolution.fitness;
        result.transformationMatrix = buildMatrix(bestSolution.parameters);
        return result;
    }
    
    private static RegistrationResult executePSO_MSE(BufferedImage model, BufferedImage scene) {
        long startTime = System.currentTimeMillis();
        PSOMSE pso = new PSOMSE(model, scene);
        PSOMSE.Particle bestSolution = pso.runPSO();
        long endTime = System.currentTimeMillis();

        if (bestSolution == null) return null;

        RegistrationResult result = new RegistrationResult();
        result.name = "PSO/MSE";
        result.fitnessType = "MSE";
        result.timeMillis = endTime - startTime;
        result.fitness = bestSolution.pBestFitness;
        result.transformationMatrix = buildMatrix(bestSolution.position);
        return result;
    }

    private static RegistrationResult executePSO_MI(BufferedImage model, BufferedImage scene) {
        long startTime = System.currentTimeMillis();
        PSOMI pso = new PSOMI(model, scene);
        PSOMI.Particle bestSolution = pso.runPSO();
        long endTime = System.currentTimeMillis();

        if (bestSolution == null) return null;

        RegistrationResult result = new RegistrationResult();
        result.name = "PSO/MI";
        result.fitnessType = "MI";
        result.timeMillis = endTime - startTime;
        result.fitness = bestSolution.pBestFitness;
        result.transformationMatrix = buildMatrix(bestSolution.position);
        return result;
    }
    
    // Escala + Rotação + Translação (Coordenadas Homogêneas):
    // [x']   [sx*cosθ  -sx*sinθ  tx] [x]
    // [y'] = [sy*sinθ   sy*cosθ  ty] [y]
    // [w']   [   0        0       1] [1]
    private static double[][] buildMatrix(double[] params) {
        return new double[][] {
            {params[0]*Math.cos(params[2]), -params[0]*Math.sin(params[2]), params[3]},
            {params[1]*Math.sin(params[2]), params[1]*Math.cos(params[2]), params[4]},
            {0,         0,         1}
        };
    }

    // Imprime os parâmetros finais no terminal:
    private static void printFinalResults(RegistrationResult[] results) {
        for (RegistrationResult result : results) {
            if (result != null) {
                System.out.printf("[%s] -> Fitness(%s): %s | Tempo: %s\n", 
                                  result.name, result.fitnessType, 
                                  result.getFormattedFitness(), result.getFormattedTime());
                System.out.printf("   Parâmetros: %s\n", Arrays.toString(result.transformationMatrix[0]) + Arrays.toString(result.transformationMatrix[1]));
            } else {
                 System.out.println("Algoritmo falhou.");
            }
        }
    }

    private static void showComparisonWindow(BufferedImage model, BufferedImage scene, RegistrationResult[] results) {
        JFrame frame = new JFrame("Comparação de Registro (4 Modelos)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Modelo + Cena + 4 Modelos = 6 painéis (2 linhas de 3):
        JPanel panel = new JPanel(new GridLayout(2, 3, 10, 10));
        
        panel.add(new ImagePanel(model, "0. Modelo (Fixed)", ""));
        panel.add(new ImagePanel(scene, "1. Cena (Moving)", ""));
        
        for (int i = 0; i < results.length; i++) {
            RegistrationResult res = results[i];
            if (res != null) {
                BufferedImage registered = ImageTransforms.applyTransform(scene, res.transformationMatrix);
                panel.add(new ImagePanel(registered, (i+2) + ". " + res.name, "Tempo: " + res.getFormattedTime()));
                ImageTransforms.saveImage(registered, "images/registeredImage_" + res.name.replace("/", "_") + "_Result.png");
            } else {
                panel.add(new ImagePanel(null, (i+2) + ". Falha", ""));
            }
        }
        
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    // Subclasse para exibir imagens e tempo:
    static class ImagePanel extends JPanel {
        private BufferedImage image;
        private String title;
        private String subtitle;

        private static final int BOX_SIZE = 300;  

        public ImagePanel(BufferedImage img, String title, String subtitle) {
            this.image = img;
            this.title = title;
            this.subtitle = subtitle;

            setPreferredSize(new Dimension(BOX_SIZE + 20, BOX_SIZE + 50));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int x = 10;
            int y = 10;

            if (image != null) {
                g.drawImage(image, x, y, BOX_SIZE, BOX_SIZE, this);

                g.setColor(Color.BLACK);
                g.drawString(title, x, y + BOX_SIZE + 25);
                g.drawString(subtitle, x, y + BOX_SIZE + 40);

            } else {
                g.setColor(Color.RED);
                g.drawString("Imagem não disponível", x + 50, y + 50);

                g.setColor(Color.BLACK);
                g.drawString(title, x, y + BOX_SIZE + 25);
            }
        }
    }

}