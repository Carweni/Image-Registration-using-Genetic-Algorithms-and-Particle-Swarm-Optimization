import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// Classe que implementa o Algoritmo Genético com MI como função objetivo:
public class GeneticMI {
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 50; 
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.8;
    private static final int NUM_PARAMETERS = 5; // Parâmetros da Transformação Afim
    private static final int NUM_BINS = 32;     // Número de caixas (bins) para o Histograma Conjunto (MI)
    
    // Intervalos de busca para os parâmetros de transformação:
    private static final double[] MIN_BOUNDS = {0.1, 0.1, -90, -150, -150}; 
    private static final double[] MAX_BOUNDS = {2.0, 2.0, 90, 150, 150}; 

    private BufferedImage modelImage; 
    private BufferedImage sceneImage; 
    private Random random;

    // Estrutura para o indivíduo/solução:
    static class Individual {
        double[] parameters; // Cromossomo 
        double fitness;    // Valor MI 
        
        public Individual(double[] params) {
            this.parameters = params;
        }
    }

    public GeneticMI(BufferedImage model, BufferedImage scene) {
        this.modelImage = model;
        this.sceneImage = scene;
        this.random = new Random();
    }

    // Converte RGB para um valor de intensidade quantizado (0 a NUM_BINS-1)
    private static int getQuantizedIntensity(int rgb) {
        // Extrai componentes RGB
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        // Converte para escala de cinza (simples média)
        int gray = (r + g + b) / 3;
        
        // Quantiza para o número de bins
        return Math.min(NUM_BINS - 1, (int) (gray * NUM_BINS / 256.0));
    }

    // Calcula o Mutual Information (MI) entre a imagem transformada e o modelo.
    private double calculateMI(double[] params) {
        double[][] affineMatrix = ImageTransforms.createAffineMatrix(params[0], params[1],params[2],params[3],params[4]);

        BufferedImage transformedScene = ImageTransforms.applyTransform(sceneImage, affineMatrix);

        int w = Math.min(modelImage.getWidth(), transformedScene.getWidth());
        int h = Math.min(modelImage.getHeight(), transformedScene.getHeight());

        long[][] jointHistogram = new long[NUM_BINS][NUM_BINS];
        long totalPixels = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Obtém a intensidade quantizada de Model (a) e Scene (b)
                int modelIntensity = getQuantizedIntensity(modelImage.getRGB(x, y));
                int sceneIntensity = getQuantizedIntensity(transformedScene.getRGB(x, y));
                
                jointHistogram[modelIntensity][sceneIntensity]++;
                totalPixels++;
            }
        }
        
        if (totalPixels == 0) return 0.0;

        double[][] pAB = new double[NUM_BINS][NUM_BINS];
        double[] pA = new double[NUM_BINS]; // Marginal de Model
        double[] pB = new double[NUM_BINS]; // Marginal de Scene
        
        for (int i = 0; i < NUM_BINS; i++) {
            for (int j = 0; j < NUM_BINS; j++) {
                pAB[i][j] = (double) jointHistogram[i][j] / totalPixels;
                pA[i] += pAB[i][j];
                pB[j] += pAB[i][j]; 
            }
        }

        // Calcula o MI:
        double mi = 0.0;
        for (int i = 0; i < NUM_BINS; i++) {
            for (int j = 0; j < NUM_BINS; j++) {
                if (pAB[i][j] > 1e-10 && pA[i] > 1e-10 && pB[j] > 1e-10) {
                    mi += pAB[i][j] * Math.log(pAB[i][j] / (pA[i] * pB[j]));
                }
            }
        }
        
        return mi / Math.log(2); 
    }

    // --- FUNÇÕES PRINCIPAIS DO GA ---

    // 1. Inicializa a população com parâmetros aleatórios dentro dos limites
    private Individual[] initializePopulation() {
        Individual[] population = new Individual[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            double[] params = new double[NUM_PARAMETERS];
            for (int j = 0; j < NUM_PARAMETERS; j++) {
                params[j] = MIN_BOUNDS[j] + (MAX_BOUNDS[j] - MIN_BOUNDS[j]) * random.nextDouble();
            }
            population[i] = new Individual(params);
        }
        return population;
    }

    // 2. Calcula o valor Fitness (MI) para cada indivíduo
    private void evaluatePopulation(Individual[] population) {
        for (Individual individual : population) {
            individual.fitness = calculateMI(individual.parameters);
        }
    }

    // 3. Seleção (Tournament Selection - Torneio)
    private Individual selectParent(Individual[] population) {
        int tournamentSize = 5;
        Individual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Individual current = population[random.nextInt(POPULATION_SIZE)];
            // Como MI (fitness) é maximizado, o maior é o melhor:
            if (best == null || current.fitness > best.fitness) {
                best = current;
            }
        }
        return best;
    }

    // 4. Crossover (BLX-alpha - Blend Crossover)
    private Individual crossover(Individual parent1, Individual parent2) {
        double[] childParams = new double[NUM_PARAMETERS];
        
        if (random.nextDouble() < CROSSOVER_RATE) {
            double alpha = 0.5;
            for (int i = 0; i < NUM_PARAMETERS; i++) {
                double min = Math.min(parent1.parameters[i], parent2.parameters[i]);
                double max = Math.max(parent1.parameters[i], parent2.parameters[i]);
                double range = max - min;
                
                double lower = min - alpha * range;
                double upper = max + alpha * range;

                childParams[i] = lower + random.nextDouble() * (upper - lower);
                
                // Limita o parâmetro ao range de busca global:
                childParams[i] = Math.max(MIN_BOUNDS[i], Math.min(MAX_BOUNDS[i], childParams[i]));
            }
        } else {
            // Se não houver crossover, um dos pais é escolhido (elitismo)
            childParams = (random.nextBoolean() ? parent1.parameters : parent2.parameters).clone();
        }
        
        return new Individual(childParams);
    }

    // 5. Mutação (Adição de ruído Gaussiano)
    private void mutate(Individual individual) {
        for (int i = 0; i < NUM_PARAMETERS; i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                // Adiciona um pequeno ruído Gaussiano (sigma=0.05)
                individual.parameters[i] += random.nextGaussian() * 0.05;

                // Limita o parâmetro ao range de busca:
                individual.parameters[i] = Math.max(MIN_BOUNDS[i], Math.min(MAX_BOUNDS[i], individual.parameters[i]));
            }
        }
    }

    // --- FUNÇÃO DE OTIMIZAÇÃO PRINCIPAL ---
    public Individual runGA() {
        Individual[] population = initializePopulation();
        evaluatePopulation(population);

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            // Ordena a população (o melhor, maior MI, fica na primeira posição)
            // Usamos -i.fitness para ordenar de forma decrescente
            Arrays.sort(population, Comparator.comparingDouble(i -> -i.fitness)); 
            Individual bestIndividual = population[0];
            
            // Log de progresso:
            System.out.printf("Geração %d: Melhor MI = %.6f (Bits)\n", generation, bestIndividual.fitness);
            
            // Condição de parada opcional:
            if (bestIndividual.fitness > 3.0) { 
                System.out.println("Convergência de MI atingida.");
                break;
            }

            Individual[] newPopulation = new Individual[POPULATION_SIZE];
            // Elitismo: Mantém o melhor indivíduo da geração anterior:
            newPopulation[0] = bestIndividual;

            // Gera o restante da nova população:
            for (int i = 1; i < POPULATION_SIZE; i++) {
                Individual parent1 = selectParent(population);
                Individual parent2 = selectParent(population);
                Individual child = crossover(parent1, parent2);
                mutate(child);
                newPopulation[i] = child;
            }

            population = newPopulation;
            evaluatePopulation(population);
        }

        // Retorna o melhor indivíduo após todas as gerações:
        Arrays.sort(population, Comparator.comparingDouble(i -> -i.fitness));
        return population[0];
    }
    
    // --- FUNÇÃO MAIN PARA TESTE ---
    public static void main(String[] args) {
        // --- 1. CONFIGURAÇÃO DE IMAGENS DE TESTE ---
        
        String modelPath = "images/fixed.png";
        String scenePath = "images/moving.png";

        BufferedImage modelImage = ImageTransforms.loadImage(modelPath);
        BufferedImage sceneImage = ImageTransforms.loadImage(scenePath);

        if (modelImage == null || sceneImage == null) {
            System.err.println("Erro: Não foi possível carregar as imagens.");
            return;
        }

        System.out.printf("Iniciando GA com MI. Modelo: %dx%d, Cena: %dx%d\n", 
                          modelImage.getWidth(), modelImage.getHeight(), 
                          sceneImage.getWidth(), sceneImage.getHeight());

        // --- 2. EXECUÇÃO DO ALGORITMO GENÉTICO ---
        
        long startTime = System.currentTimeMillis();
        GeneticMI ga = new GeneticMI(modelImage, sceneImage);
        Individual bestSolution = ga.runGA();
        long endTime = System.currentTimeMillis();

        double[][] resultMatrix = ImageTransforms.createAffineMatrix(bestSolution.parameters[0], bestSolution.parameters[1],bestSolution.parameters[2],bestSolution.parameters[3],bestSolution.parameters[4]);

        // Aplica a solução encontrada para obter a imagem registrada:
        BufferedImage registeredImage = ImageTransforms.applyTransform(sceneImage, resultMatrix);
        
        // Salva o resultado na pasta 'images'
        ImageTransforms.saveImage(registeredImage, "images/registeredImage_GA_MI_Result.png");

        System.out.println("\n--- Solução Encontrada (GA/MI) ---");
        System.out.printf("Melhor MI: %.6f (Bits)\n", bestSolution.fitness);
        System.out.printf("Tempo de execução: %.2f segundos\n", (endTime - startTime) / 1000.0);
        System.out.println("Parâmetros Afins Encontrados (sx, sy, theta, tx, ty):");
        System.out.println(Arrays.toString(bestSolution.parameters));
        System.out.println("---------------------------------");
        
        // --- 4. VISUALIZAÇÃO ---
        SwingUtilities.invokeLater(() -> showImagesWindow(modelImage, sceneImage, registeredImage));
    }
    
    // Função para mostrar as imagens de teste
    private static void showImagesWindow(BufferedImage model, BufferedImage scene, BufferedImage registered) {
        JFrame frame = new JFrame("Registro de Imagem GA/MI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10)); // 1 linha, 3 colunas
        
        panel.add(new ImagePanel(model, "Modelo (Fixed.png)"));
        panel.add(new ImagePanel(scene, "Cena (Moving.png)"));
        panel.add(new ImagePanel(registered, "Registrada (Solução GA/MI)"));
        
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}