import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// Classe que implementa o Particle Swarm Optimization (PSO) com MSE como função objetivo.
public class PSOMSE {
    private static final int SWARM_SIZE = 50;  // Tamanho do enxame.
    private static final int MAX_ITERATIONS = 50; 
    private static final int NUM_PARAMETERS = 5; // Número de parâmetros da Transformação Afim.
    
    private static final double W_MAX = 0.9;  // Inércia máxima (controla a exploração)
    private static final double W_MIN = 0.4;  // Inércia mínima
    private static final double C1 = 2.0;     // Coeficiente cognitivo (pBest - influência da melhor posição individual)
    private static final double C2 = 2.0;     // Coeficiente social (gBest - influência da melhor posição global)

    // Intervalos de busca:
    private static final double[] MIN_BOUNDS = {0.1, 0.1, -90, -150, -150}; 
    private static final double[] MAX_BOUNDS = {2.0, 2.0, 90, 150, 150}; 
    private static final double V_MAX = 0.1; // Velocidade máxima

    private BufferedImage modelImage;
    private BufferedImage sceneImage;
    private Random random;

    // Estrutura para a partícula (solução candidata no espaço de busca):
    static class Particle {
        double[] position;     // Posição atual
        double[] velocity;     // Velocidade atual da partícula(direção e magnitude do movimento)
        double[] pBestPosition; // Melhor posição individual encontrada (pBest)
        double pBestFitness;   // Melhor fitness do pBest
        double currentFitness; // Fitness atual
        
        public Particle(double[] pos, double[] vel) {
            this.position = pos;
            this.velocity = vel;
            this.pBestPosition = pos.clone();
            this.pBestFitness = Double.MAX_VALUE;
        }
    }

    // Construtor do otimizador:
    public PSOMSE(BufferedImage model, BufferedImage scene) {
        this.modelImage = model;
        this.sceneImage = scene;
        this.random = new Random();
    }

    // Função objetivo (MSE):
    private double calculateMSE(double[] params) {
        // Cria a matriz de transformação afim a partir dos parâmetros:
        double[][] affineMatrix = ImageTransforms.createAffineMatrix(params[0], params[1],params[2],params[3],params[4]);

        BufferedImage transformedScene = ImageTransforms.applyTransform(sceneImage, affineMatrix);

        long sumSquaredError = 0;
        int count = 0;

        // Determina a região de sobreposição entre as imagens:
        int w = Math.min(modelImage.getWidth(), transformedScene.getWidth());
        int h = Math.min(modelImage.getHeight(), transformedScene.getHeight());

        // Calcula a soma dos erros quadráticos para cada canal RGB:
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int modelRGB = modelImage.getRGB(x, y);
                int sceneRGB = transformedScene.getRGB(x, y);

                int rModel = (modelRGB >> 16) & 0xFF;
                int gModel = (modelRGB >> 8) & 0xFF;
                int bModel = modelRGB & 0xFF;

                int rScene = (sceneRGB >> 16) & 0xFF;
                int gScene = (sceneRGB >> 8) & 0xFF;
                int bScene = sceneRGB & 0xFF;
                
                sumSquaredError += Math.pow(rModel - rScene, 2);
                sumSquaredError += Math.pow(gModel - gScene, 2);
                sumSquaredError += Math.pow(bModel - bScene, 2);
                
                count += 3;
            }
        }

        // Retorna a média dos erros quadráticos:
        return (double) sumSquaredError / count;
    }

    // Inicializa o enxame de partículas:
    private Particle[] initializeSwarm() {
        Particle[] swarm = new Particle[SWARM_SIZE];
        for (int i = 0; i < SWARM_SIZE; i++) {
            double[] pos = new double[NUM_PARAMETERS];
            double[] vel = new double[NUM_PARAMETERS];
            
            for (int j = 0; j < NUM_PARAMETERS; j++) {
                // Posição inicial aleatória dentro dos limites:
                pos[j] = MIN_BOUNDS[j] + (MAX_BOUNDS[j] - MIN_BOUNDS[j]) * random.nextDouble();
                vel[j] = (random.nextDouble() * 2 * V_MAX) - V_MAX;
            }
            swarm[i] = new Particle(pos, vel);
            swarm[i].currentFitness = calculateMSE(pos);
            swarm[i].pBestFitness = swarm[i].currentFitness;
        }
        return swarm;
    }

    // Encontra a melhor posição global (gBest) em todo o enxame:
    private double[] findGBest(Particle[] swarm) {
        double gBestFitness = Double.MAX_VALUE;
        double[] gBestPosition = null;

        for (Particle p : swarm) {
            if (p.pBestFitness < gBestFitness) {
                gBestFitness = p.pBestFitness;
                gBestPosition = p.pBestPosition;
            }
        }
        return gBestPosition;
    }

    // Função de otimização:
    public Particle runPSO() {
        Particle[] swarm = initializeSwarm();
        double[] gBestPosition = findGBest(swarm);
        double gBestFitness = calculateMSE(gBestPosition);
        
        System.out.printf("Início do PSO: Melhor MSE = %.6f\n", gBestFitness);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Fator de inércia que decai linearmente:
            double w = W_MAX - iteration * (W_MAX - W_MIN) / MAX_ITERATIONS;

            for (Particle p : swarm) {
                for (int i = 0; i < NUM_PARAMETERS; i++) {
                    // Fatores de aceleração aleatórios:
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();

                    // Cálculo da nova Velocidade:
                    double cognitiveComponent = C1 * r1 * (p.pBestPosition[i] - p.position[i]); // Atrai para pBest.
                    double socialComponent = C2 * r2 * (gBestPosition[i] - p.position[i]);      // Atrai para gBest.
                    
                    // Atualiza velocidade:
                    p.velocity[i] = w * p.velocity[i] + cognitiveComponent + socialComponent;
                    p.velocity[i] = Math.max(-V_MAX, Math.min(V_MAX, p.velocity[i]));

                    // Cálculo da nova Posição (X_new = X_old + V_new):
                    p.position[i] += p.velocity[i];

                    // Aplica restrições de limite:
                    p.position[i] = Math.max(MIN_BOUNDS[i], Math.min(MAX_BOUNDS[i], p.position[i]));
                }

                // Avalia o Fitness e atualiza pBest:
                p.currentFitness = calculateMSE(p.position);

                // Atualização do melhor individual:
                if (p.currentFitness < p.pBestFitness) { // Minimizar MSE
                    p.pBestFitness = p.currentFitness;
                    p.pBestPosition = p.position.clone();
                    
                    // Atualização do melhor global:
                    if (p.pBestFitness < gBestFitness) {
                        gBestFitness = p.pBestFitness;
                        gBestPosition = p.pBestPosition.clone();
                    }
                }
            }
            
            System.out.printf("Iteração %d: Melhor MSE = %.6f\n", iteration, gBestFitness);

            // Critério de parada por convergência:
            if (gBestFitness < 1.0) { 
                System.out.println("Convergência atingida.");
                break;
            }
        }
        
        // Retorna a melhor partícula:
        Particle bestParticle = new Particle(gBestPosition.clone(), new double[NUM_PARAMETERS]);
        bestParticle.pBestFitness = gBestFitness;
        return bestParticle;
    }

    // Função para mostrar as imagens:
    private static void showImagesWindow(BufferedImage model, BufferedImage scene, BufferedImage registered) {
        JFrame frame = new JFrame("Registro de Imagem PSO/MSE");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10)); // 1 linha, 3 colunas
        
        panel.add(new ImagePanel(model, "Modelo (Fixed.png)"));
        panel.add(new ImagePanel(scene, "Cena (Moving.png)"));
        panel.add(new ImagePanel(registered, "Registrada (Solução PSO)"));
        
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {
        String modelPath = "images/fixed.png";
        String scenePath = "images/moving.png";

        BufferedImage modelImage = ImageTransforms.loadImage(modelPath);
        BufferedImage sceneImage = ImageTransforms.loadImage(scenePath);

        if (modelImage == null || sceneImage == null) {
            System.err.println("Erro: Não foi possível carregar as imagens.");
            return;
        }

        System.out.printf("Iniciando PSO com MSE.\n");

        long startTime = System.currentTimeMillis();
        PSOMSE pso = new PSOMSE(modelImage, sceneImage);
        Particle bestSolution = pso.runPSO();
        long endTime = System.currentTimeMillis();

        double[][] resultMatrix = ImageTransforms.createAffineMatrix(bestSolution.position[0], bestSolution.position[1],bestSolution.position[2],bestSolution.position[3],bestSolution.position[4]);

        BufferedImage registeredImage = ImageTransforms.applyTransform(sceneImage, resultMatrix);
        ImageTransforms.saveImage(registeredImage, "images/registeredImage_PSO_MSE_Result.png");

        System.out.println("\n--- Solução Encontrada (PSO/MSE) ---");
        System.out.printf("Melhor MSE: %.6f\n", bestSolution.pBestFitness);
        System.out.printf("Tempo de execução: %.2f segundos\n", (endTime - startTime) / 1000.0);
        System.out.println("Parâmetros Afins Encontrados (sx, sy, theta, tx, ty):");
        System.out.println(Arrays.toString(bestSolution.position));
        System.out.println("---------------------------------");
        
        SwingUtilities.invokeLater(() -> showImagesWindow(modelImage, sceneImage, registeredImage));
    }
}