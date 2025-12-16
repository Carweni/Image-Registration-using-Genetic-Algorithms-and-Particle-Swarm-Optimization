import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// Classe que implementa o Particle Swarm Optimization com MI como função objetivo.
public class PSOMI {
    private static final int SWARM_SIZE = 50;       // Tamanho do enxame.
    private static final int MAX_ITERATIONS = 50; 
    private static final int NUM_PARAMETERS = 5; // Número de parâmetros da Transformação Afim
    private static final int NUM_BINS = 32;     // Número de bins para o histograma conjunto.
    
    private static final double W_MAX = 0.9;  // Inércia máxima  (controla a exploração)
    private static final double W_MIN = 0.4;  // Inércia mínima
    private static final double C1 = 2.0;      // Coeficiente cognitivo (pBest - influência da melhor posição individual)
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
            this.pBestFitness = Double.MIN_VALUE; 
        }
    }

    // Construtor do otimizador:
    public PSOMI(BufferedImage model, BufferedImage scene) {
        this.modelImage = model;
        this.sceneImage = scene;
        this.random = new Random();
    }

    // Converte RGB para um valor de intensidade quantizado (0 a NUM_BINS-1):
    private static int getQuantizedIntensity(int rgb) {
        // Extrai componentes RGB:
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        // Converte para escala de cinza usando média simples:
        int gray = (r + g + b) / 3;
        
        // Quantiza para o número de bins:
        return Math.min(NUM_BINS - 1, (int) (gray * NUM_BINS / 256.0));
    }

    // Função objetivo: Calcula a Mutual Information (MI) entre as imagens.
    private double calculateMI(double[] params) {
        // Aplica a transformação afim:
        double[][] affineMatrix = ImageTransforms.createAffineMatrix(params[0], params[1],params[2],params[3],params[4]);

        BufferedImage transformedScene = ImageTransforms.applyTransform(sceneImage, affineMatrix);

        // Determina a região de sobreposição:
        int w = Math.min(modelImage.getWidth(), transformedScene.getWidth());
        int h = Math.min(modelImage.getHeight(), transformedScene.getHeight());
        
        // Cria o Histograma Conjunto:
        long[][] jointHistogram = new long[NUM_BINS][NUM_BINS];
        long totalPixels = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Obtém a intensidades quantizadas:
                int modelIntensity = getQuantizedIntensity(modelImage.getRGB(x, y));
                int sceneIntensity = getQuantizedIntensity(transformedScene.getRGB(x, y));
                
                jointHistogram[modelIntensity][sceneIntensity]++;
                totalPixels++;
            }
        }
        
        if (totalPixels == 0) return 0.0;

        // Calcula distribuições de probabilidade:
        double[][] pAB = new double[NUM_BINS][NUM_BINS];  // Probabilidade conjunta P(A,B)
        double[] pA = new double[NUM_BINS]; // Probabilidade marginal P(A) - imagem fixa.
        double[] pB = new double[NUM_BINS];  // Probabilidade marginal P(B) - imagem móvel.
        
        for (int i = 0; i < NUM_BINS; i++) {
            for (int j = 0; j < NUM_BINS; j++) {
                pAB[i][j] = (double) jointHistogram[i][j] / totalPixels;
                pA[i] += pAB[i][j];
                pB[j] += pAB[i][j]; 
            }
        }

        // Calcula a Mutual Information (MI):
        double mi = 0.0;
        for (int i = 0; i < NUM_BINS; i++) {
            for (int j = 0; j < NUM_BINS; j++) {
                if (pAB[i][j] > 1e-10 && pA[i] > 1e-10 && pB[j] > 1e-10) {
                    mi += pAB[i][j] * Math.log(pAB[i][j] / (pA[i] * pB[j]));
                }
            }
        }
        
        // Converte de base e para base 2:
        return mi / Math.log(2); 
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
            swarm[i].currentFitness = calculateMI(pos);
            swarm[i].pBestFitness = swarm[i].currentFitness;
        }
        return swarm;
    }

    // Encontra a melhor posição global (gBest):
    private double[] findGBest(Particle[] swarm) {
        double gBestFitness = Double.MIN_VALUE; 
        double[] gBestPosition = null;

        for (Particle p : swarm) {
            if (p.pBestFitness > gBestFitness) { 
                gBestFitness = p.pBestFitness;
                gBestPosition = p.pBestPosition;
            }
        }
        return gBestPosition;
    }

    // Executa o algoritmo PSO para maximizar o Mutual Information:
    public Particle runPSO() {
        Particle[] swarm = initializeSwarm();
        double[] gBestPosition = findGBest(swarm);
        double gBestFitness = calculateMI(gBestPosition);
        
        System.out.printf("Início PSO: Melhor MI = %.6f (Bits)\n", gBestFitness);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Fator de inércia que decai linearmente:
            double w = W_MAX - iteration * (W_MAX - W_MIN) / MAX_ITERATIONS;

            for (Particle p : swarm) {
                // Atualização de velocidade e posição para cada parâmetro:
                for (int i = 0; i < NUM_PARAMETERS; i++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();

                    // Cálculo da nova Velocidade:
                    double cognitiveComponent = C1 * r1 * (p.pBestPosition[i] - p.position[i]);
                    double socialComponent = C2 * r2 * (gBestPosition[i] - p.position[i]);
                    
                    p.velocity[i] = w * p.velocity[i] + cognitiveComponent + socialComponent;
                    p.velocity[i] = Math.max(-V_MAX, Math.min(V_MAX, p.velocity[i]));

                    // Cálculo da nova posição (X_new = X_old + V_new):
                    p.position[i] += p.velocity[i];

                    // Aplica restrições de limite (clamp):
                    p.position[i] = Math.max(MIN_BOUNDS[i], Math.min(MAX_BOUNDS[i], p.position[i]));
                }

                // Avalia o Fitness e atualiza pBest:
                p.currentFitness = calculateMI(p.position);
                if (p.currentFitness > p.pBestFitness) { 
                    p.pBestFitness = p.currentFitness;
                    p.pBestPosition = p.position.clone();
                    
                    // Atualiza gBest se necessário:
                    if (p.pBestFitness > gBestFitness) { 
                        gBestFitness = p.pBestFitness;
                        gBestPosition = p.pBestPosition.clone();
                    }
                }
            }

            System.out.printf("Iteração %d: Melhor MI = %.6f (Bits)\n", iteration, gBestFitness);

            if (gBestFitness > 3.0) { 
                System.out.println("Convergência atingida.");
                break;
            }
        }
        
        // Retorna a melhor partícula (que contém o gBest):
        Particle bestParticle = new Particle(gBestPosition.clone(), new double[NUM_PARAMETERS]);
        bestParticle.pBestFitness = gBestFitness;
        return bestParticle;
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

        System.out.printf("Iniciando PSO com MI. Modelo: %dx%d, Cena: %dx%d\n", 
                          modelImage.getWidth(), modelImage.getHeight(), 
                          sceneImage.getWidth(), sceneImage.getHeight());

        // Execução do PSO:
        long startTime = System.currentTimeMillis();
        PSOMI pso = new PSOMI(modelImage, sceneImage);
        Particle bestSolution = pso.runPSO();
        long endTime = System.currentTimeMillis();

        double[][] resultMatrix = ImageTransforms.createAffineMatrix(bestSolution.position[0], bestSolution.position[1],bestSolution.position[2],bestSolution.position[3],bestSolution.position[4]);

        BufferedImage registeredImage = ImageTransforms.applyTransform(sceneImage, resultMatrix);
        ImageTransforms.saveImage(registeredImage, "images/registeredImage_PSO_MI_Result.png");

        System.out.println("\n--- Solução Encontrada (PSO/MI) ---");
        System.out.printf("Melhor MI: %.6f (Bits)\n", bestSolution.pBestFitness);
        System.out.printf("Tempo de execução: %.2f segundos\n", (endTime - startTime) / 1000.0);
        System.out.println("Parâmetros Afins Encontrados (sx, sy, theta, tx, ty):");
        System.out.println(Arrays.toString(bestSolution.position));
        System.out.println("---------------------------------");
        
        SwingUtilities.invokeLater(() -> showImagesWindow(modelImage, sceneImage, registeredImage));
    }
    
    // Função para mostrar as imagens:
    private static void showImagesWindow(BufferedImage model, BufferedImage scene, BufferedImage registered) {
        JFrame frame = new JFrame("Registro de Imagem PSO/MI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10)); // 1 linha, 3 colunas
        
        panel.add(new ImagePanel(model, "Modelo (Fixed.png)"));
        panel.add(new ImagePanel(scene, "Cena (Moving.png)"));
        panel.add(new ImagePanel(registered, "Registrada (Solução PSO/MI)"));
        
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}