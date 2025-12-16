import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// Classe que implementa o Algoritmo Genético com a métrica Mean Squared Error (MSE) como função objetivo.
public class GeneticMSE {
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 50; 
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.8;
    private static final int NUM_PARAMETERS = 5; //sx,sy, theta, tx, ty
    
    // Intervalos de busca para os parâmetros de transformação:
    //sx,sy, theta, tx, ty
    private static final double[] MIN_BOUNDS = {0.1, 0.1, -90, -150, -150}; 
    private static final double[] MAX_BOUNDS = {2.0, 2.0, 90, 150, 150};
    
    private BufferedImage modelImage; 
    private BufferedImage sceneImage; 
    
    private Random random;

    // Estrutura para o indivíduo:
    public static class Individual {
        private double[] parameters;
        private double fitness;

        public Individual(double[] params) {
            this.parameters = params;
        }

        public double[] getParameters() {
            return parameters;
        }

        public double getFitness() {
            return fitness;
        }

        public void setFitness(double fitness) {
            this.fitness = fitness;
        }
    }

    public GeneticMSE(BufferedImage model, BufferedImage scene) {
        this.modelImage = model;
        this.sceneImage = scene;
        this.random = new Random();
    }

    // Inicializa a população com parâmetros aleatórios dentro dos limites:
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

    // Calcula o valor do MSE para cada indivíduo:
    private void evaluatePopulation(Individual[] population) {
        for (Individual individual : population) {
            individual.setFitness(calculateMSE(individual.getParameters()));
        }
    }

    // Função Objetivo: Calcula o MSE entre a imagem transformada e o modelo.
    private double calculateMSE(double[] params) {    //sx,sy, theta, tx, ty
        double[][] affineMatrix = ImageTransforms.createAffineMatrix(params[0], params[1],params[2],params[3],params[4]);

        // Aplica a transformação inversa:
        BufferedImage transformedScene = ImageTransforms.applyTransform(sceneImage, affineMatrix);

        // Calcula o MSE entre a imagem transformada e o modelo:
        long sumSquaredError = 0;
        int count = 0;
        int w = Math.min(modelImage.getWidth(), transformedScene.getWidth());
        int h = Math.min(modelImage.getHeight(), transformedScene.getHeight());

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int modelRGB = modelImage.getRGB(x, y);
                int sceneRGB = transformedScene.getRGB(x, y);

                // Separa os canais de cor:
                int rModel = (modelRGB >> 16) & 0xFF;
                int gModel = (modelRGB >> 8) & 0xFF;
                int bModel = modelRGB & 0xFF;

                int rScene = (sceneRGB >> 16) & 0xFF;
                int gScene = (sceneRGB >> 8) & 0xFF;
                int bScene = sceneRGB & 0xFF;
                
                // Erro Quadrático Total (todas as cores):
                sumSquaredError += Math.pow(rModel - rScene, 2);
                sumSquaredError += Math.pow(gModel - gScene, 2);
                sumSquaredError += Math.pow(bModel - bScene, 2);
                
                count += 3; // 3 canais (R, G e b) por pixel.
            }
        }

        if (count == 0) {
            return Double.MAX_VALUE; 
        }

        // Retorna o MSE:
        return (double) sumSquaredError / count;
    }

    // Seleção por torneio:
    private Individual selectParent(Individual[] population) {
        int tournamentSize = 5;
        Individual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Individual current = population[random.nextInt(POPULATION_SIZE)];
            if (best == null || current.fitness < best.fitness) {
                best = current;
            }
        }
        return best;
    }

    // Crossover:
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
            // Se não houver crossover, um dos pais é escolhido:
            childParams = (random.nextBoolean() ? parent1.parameters : parent2.parameters).clone();
        }
        
        return new Individual(childParams);
    }

    // Mutação com ruído Gaussiano:
    private void mutate(Individual individual) {
        for (int i = 0; i < NUM_PARAMETERS; i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                // Adiciona um pequeno ruído Gaussiano:
                individual.parameters[i] += random.nextGaussian() * 0.05;

                // Limita o parâmetro ao range de busca:
                individual.parameters[i] = Math.max(MIN_BOUNDS[i], Math.min(MAX_BOUNDS[i], individual.parameters[i]));
            }
        }
    }

    // Função de otimização (roda o algoritmo):
    public Individual runGA() {
        Individual[] population = initializePopulation();
        evaluatePopulation(population);

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            // Ordena a população (o menor MSE, fica na primeira posição):
            Arrays.sort(population, Comparator.comparingDouble(i -> i.fitness));
            Individual bestIndividual = population[0];
            
            Individual[] newPopulation = new Individual[POPULATION_SIZE];

            // Elitismo (Mantém o melhor indivíduo da geração anterior):
            newPopulation[0] = bestIndividual;

            System.out.printf("Geração %d: Melhor MSE = %.6f\n", generation, bestIndividual.fitness);

            // Gera o restante da nova população::
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
        Arrays.sort(population, Comparator.comparingDouble(i -> i.fitness));
        return population[0];
    }

    // Função para mostrar as imagens de teste:
    private static void showImagesWindow(BufferedImage model, BufferedImage scene, BufferedImage registered) {
        JFrame frame = new JFrame("Registro de Imagem GA/MSE");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10)); // 1 linha, 3 colunas
        
        panel.add(new ImagePanel(model, "Modelo (Fixed.png)"));
        panel.add(new ImagePanel(scene, "Cena (Moving.png)"));
        panel.add(new ImagePanel(registered, "Registrada (Solução GA)"));
        
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

        // Execução do G.A.: 
        long startTime = System.currentTimeMillis();
        GeneticMSE ga = new GeneticMSE(modelImage, sceneImage);
        Individual bestSolution = ga.runGA();
        long endTime = System.currentTimeMillis();

        double[] params = bestSolution.getParameters();

        double[][] resultMatrix = ImageTransforms.createAffineMatrix(
            params[0], params[1], params[2], params[3], params[4]
        );

        // Aplica a solução encontrada para obter a imagem registrada e a salva:
        BufferedImage registeredImage = ImageTransforms.applyTransform(sceneImage, resultMatrix);
        ImageTransforms.saveImage(registeredImage, "images/registeredImage_GA_MSE_Result.png");

        System.out.println("\n--- Solução Encontrada (GA) ---");
        System.out.printf("Melhor MSE: %.6f\n", bestSolution.getFitness());
        System.out.printf("Tempo de execução: %.2f segundos\n", (endTime - startTime) / 1000.0);
        System.out.println("Parâmetros Afins Encontrados (sx, sy, theta, tx, ty):");
        System.out.println(Arrays.toString(params));
        System.out.println("---------------------------------");
        
        // Visualização:
        SwingUtilities.invokeLater(() -> showImagesWindow(modelImage, sceneImage, registeredImage));
    }
}