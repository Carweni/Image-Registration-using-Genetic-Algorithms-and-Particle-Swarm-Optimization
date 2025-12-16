# Image Registration using Genetic Algorithms and Particle Swarm Optimization

Um sistema de registro de imagens implementado em Java que compara diferentes algoritmos de otimização metaheurística (Algoritmos Genéticos e Particle Swarm Optimization) com diferentes métricas de similaridade (MSE e Mutual Information).

## Sobre o Projeto

Este projeto implementa e compara quatro abordagens diferentes para o problema de registro de imagens:

1. **GA/MSE** - Algoritmo Genético com Mean Squared Error
2. **GA/MI** - Algoritmo Genético com Mutual Information
3. **PSO/MSE** - Particle Swarm Optimization com Mean Squared Error
4. **PSO/MI** - Particle Swarm Optimization com Mutual Information

O objetivo é encontrar os parâmetros ideais de transformação afim (escala, rotação e translação) que melhor alinham uma imagem móvel a uma imagem fixa.

## Tecnologias

- Java
- Java AWT/Swing para visualização
- BufferedImage para manipulação de imagens

## 📁 Estrutura do Projeto

```
.
├── GeneticMSE.java        
├── GeneticMI.java         
├── PSOMSE.java            
├── PSOMI.java             
├── ImageTransforms.java   
├── ImagePanel.java        
├── ImageRegistration.java 
└── images/
    ├── fixed.png          # Imagem de referência
    └── moving.png         # Imagem a ser registrada
```

## Instruções de Uso

### Pré-requisitos

- Java JDK 8 ou superior

### Compilação

```bash
javac *.java
```

### Execução

Para executar a comparação completa dos 4 algoritmos:

```bash
java ImageRegistration
```

Ou execute cada algoritmo individualmente:

```bash
java GeneticMSE
java GeneticMI
java PSOMSE
java PSOMI
```

## Parâmetros 

Cada algoritmo possui parâmetros ajustáveis no início da classe:

### Algoritmos Genéticos
- `POPULATION_SIZE`: Tamanho da população (padrão: 50)
- `MAX_GENERATIONS`: Número máximo de gerações (padrão: 50)
- `MUTATION_RATE`: Taxa de mutação (padrão: 0.1)
- `CROSSOVER_RATE`: Taxa de crossover (padrão: 0.8)

### PSO
- `SWARM_SIZE`: Tamanho do enxame (padrão: 50)
- `MAX_ITERATIONS`: Número máximo de iterações (padrão: 50)
- `W_MAX/W_MIN`: Inércia máxima/mínima (padrão: 0.9/0.4)
- `C1/C2`: Coeficientes cognitivo/social (padrão: 2.0/2.0)

### Espaço de Busca
- **Escala (sx, sy)**: 0.1 a 2.0
- **Rotação (theta)**: -90° a 90°
- **Translação (tx, ty)**: -150 a 150 pixels

## Métricas de Avaliação

### Mean Squared Error (MSE)
Calcula a diferença quadrática média entre os valores RGB dos pixels das duas imagens. **Objetivo: minimizar**.

```
MSE = Σ(I₁(x,y) - I₂(x,y))² / N
```

### Mutual Information (MI)
Mede a dependência estatística entre as distribuições de intensidade das imagens usando histogramas conjuntos. **Objetivo: maximizar**.

```
MI = ΣΣ P(a,b) × log₂(P(a,b) / (P(a) × P(b)))
```

## Resultados

Os resultados são salvos automaticamente na pasta `images/`:
- `registeredImage_GA_MSE_Result.png`
- `registeredImage_GA_MI_Result.png`
- `registeredImage_PSO_MSE_Result.png`
- `registeredImage_PSO_MI_Result.png`

O programa também exibe:
- Valores de fitness de cada algoritmo
- Tempo de execução
- Parâmetros de transformação encontrados
- Visualização comparativa lado a lado

## Considerações:

- As imagens devem estar em formato PNG
- O algoritmo usa interpolação bilinear para qualidade superior
- Pixels fora dos limites da imagem são considerados pretos
- A conversão para escala de cinza usa média simples RGB
- A quantização para MI usa 32 bins por padrão

## 📄 Licença

Este projeto está disponível para uso educacional e acadêmico.

---