import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageTransforms {
    // public static void main(String[] args) throws Exception {
    //     String inFile = args.length > 0 ? args[0] : "image.jpg";
    //     BufferedImage src = loadImage(inFile);
    //     if (src == null) {
    //         System.err.println("Erro: não foi possível abrir " + inFile);
    //         System.exit(1);
    //     }

    //     // 1) Escala:
    //     double sx = 0.75;
    //     double sy = 0.75;
    //     BufferedImage scaled = scale(src, sx, sy); 
    //     saveImage(scaled, "scaled.jpg");
        
    //     // 2) Rotação:
    //     double angleDeg = 60;
    //     BufferedImage rotated = rotation(src, Math.toRadians(angleDeg));
    //     saveImage(rotated, "rotated.jpg");

    //     // 3) Translação:
    //     int x = 200;
    //     int y = 100;
    //     BufferedImage translated = translation(src, x, y);
    //     saveImage(translated, "translated.jpg");

    //     // 4) Junção das Transformadas:
    //     double[][] affineMatrix = createAffineMatrix(0.8, 0.8, Math.toRadians(60), 100, 50);
    //     BufferedImage affine = applyTransform(src, affineMatrix);
    //     saveImage(affine, "composed_affine.jpg");

    //     // 5) Transformação projetiva/perspectiva:
    //     int w = src.getWidth();
    //     int h = src.getHeight();

    //     // Pontos originais:
    //     double[][] srcPts = {
    //         {0, 0},
    //         {w, 0},
    //         {w, h},
    //         {0, h}
    //     };

    //     // Pontos para projeção:
    //     double[][] dstPts = {
    //         { 0, 0      },         // Inferior esquerdo
    //         { w,       h*0.10 },   // Inferior direito
    //         { w,       h*1.90 },   // Superior direito
    //         { 0, h      }          // Superior esquerdo
    //     };

    //     double[][] H = computeHomography(srcPts, dstPts); 
    //     BufferedImage perspective = applyHomography(src, H, w, h);
    //     saveImage(perspective, "perspective.jpg");

    //     // Mostrar imagem original e transformadas:
    //     SwingUtilities.invokeLater(() -> showImagesWindow(src, scaled, rotated, translated, perspective, affine));
    // }

    /* FUNÇÕES PARA CARREGAR, SALVAR E MOSTRAR IMAGENS */

    // Carrega a imagem:
    public static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Salva a imagem:
    public static void saveImage(BufferedImage img, String path) {
        try {
            ImageIO.write(img, "jpg", new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Abre o painel de visualização:
    public static void showImagesWindow(BufferedImage original, BufferedImage scaled, BufferedImage rotated, BufferedImage translated, BufferedImage perspective, BufferedImage affine) {
        JFrame frame = new JFrame("Imagem Original e Imagens Transformadas");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int gap = 10;
                int x = gap;
                int y = gap;
                double displayScale = 0.5; // Escala de visualização.
                
                int w1 = (int)(original.getWidth() * displayScale);
                int h1 = (int)(original.getHeight() * displayScale);
                g.drawImage(original, x, y, w1, h1, null);
                x += w1 + gap;
                
                int w2 = (int)(scaled.getWidth() * displayScale);
                int h2 = (int)(scaled.getHeight() * displayScale);
                g.drawImage(scaled, x, y, w2, h2, null);
                x += w2 + gap;
                
                int w3 = (int)(rotated.getWidth() * displayScale);
                int h3 = (int)(rotated.getHeight() * displayScale);
                g.drawImage(rotated, x, y, w3, h3, null);
                x += w3 + gap;

                int w5 = (int)(translated.getWidth() * displayScale);
                int h5 = (int)(translated.getHeight() * displayScale);
                g.drawImage(translated, x, y, w5, h5, null);
                x += w5 + gap;
                
                int w4 = (int)(perspective.getWidth() * displayScale);
                int h4 = (int)(perspective.getHeight() * displayScale);
                g.drawImage(perspective, x, y, w4, h4, null);
                x += w4 + gap;

                int w6 = (int)(affine.getWidth() * displayScale);
                int h6 = (int)(affine.getHeight() * displayScale);
                g.drawImage(affine, x, y, w6, h6, null);
            }

            @Override
            public Dimension getPreferredSize() {
                double displayScale = 0.5;
                int w = (int)((original.getWidth() + scaled.getWidth() + rotated.getWidth() + translated.getWidth() + perspective.getWidth() + affine.getWidth()) * displayScale) + 50;
                int h = (int)(Math.max(Math.max(Math.max(Math.max(original.getHeight(), scaled.getHeight()),Math.max(rotated.getHeight(), translated.getHeight())),perspective.getHeight()), affine.getHeight()) * displayScale) + 20;
                
                return new Dimension(w, h);
            }
        };

        frame.setContentPane(new JScrollPane(panel));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /* FUNÇÕES AUXILIARES */

    // Método de interpolação bilinear - Calcula o valor de um pixel interpolando os 4 pixels vizinhos mais próximos da imagem original.
    public static int bilinearInterpolate(BufferedImage img, double x, double y) {
        // Coordenadas dos 4 vizinhos considerados:
        int x1 = (int) Math.floor(x);
        int y1 = (int) Math.floor(y);
        int x2 = x1 + 1;
        int y2 = y1 + 1;
        
        if (x < 0 || y < 0 || x >= img.getWidth()-1 || y >= img.getHeight()-1) {
            return 0x000000; // Pixels fora da imagem se tornam pretos.
        }
        
        double wx = x - x1; // Peso horizontal.
        double wy = y - y1; // Peso vertical.
        
        // Obter os 4 pixels vizinhos:
        int c11 = img.getRGB(x1, y1);
        int c12 = img.getRGB(x1, y2);
        int c21 = img.getRGB(x2, y1);
        int c22 = img.getRGB(x2, y2);
        
        // Interpolar componentes de cor:
        // Red
        int r = (int)((1-wx)*(1-wy)*((c11 >> 16) & 0xFF) +
                (1-wx)*wy*((c12 >> 16) & 0xFF) +
                wx*(1-wy)*((c21 >> 16) & 0xFF) +
                wx*wy*((c22 >> 16) & 0xFF));
        
        // Green
        int g = (int)((1-wx)*(1-wy)*((c11 >> 8) & 0xFF) +
                (1-wx)*wy*((c12 >> 8) & 0xFF) +
                wx*(1-wy)*((c21 >> 8) & 0xFF) +
                wx*wy*((c22 >> 8) & 0xFF));
        
        // Blue
         int b = (int)((1-wx)*(1-wy)*(c11 & 0xFF) +
                (1-wx)*wy*(c12 & 0xFF) +
                wx*(1-wy)*(c21 & 0xFF) +
                wx*wy*(c22 & 0xFF));
        
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        
        return (r << 16) | (g << 8) | b;
    }

    // Multiplica matriz 3x3 por vetor 3x1:
    public static double[] multiplyMatVec(double[][] M, double[] v) {
        double[] r = new double[3]; // Resulta em vetor 3x1
        for (int i = 0; i < 3; i++) {
            r[i] = M[i][0] * v[0] + M[i][1] * v[1] + M[i][2] * v[2];
        }
        return r;
    }

    // Resolve  um sistema linear Ax = b
    // A - matriz nxn dos coeficientes
    // b - vetor nx1 dos termos independentes
    public static double[] solveLinearSystem(double[][] A_in, double[] b_in) {
        int n = b_in.length;
        double[][] A = new double[n][n + 1]; 
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) A[i][j] = A_in[i][j];
            A[i][n] = b_in[i];
        }

        // Escalona a matriz para solucionar:
        for (int col = 0; col < n; col++) {
            // Encontra a linha com maior valor absoluto na coluna atual:
            int pivot = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(A[r][col]) > Math.abs(A[pivot][col])) pivot = r;
            }

            // Se o pivô é muito próximo de zero, a matriz é singular:
            if (Math.abs(A[pivot][col]) < 1e-12) throw new RuntimeException("Matriz singular");
            
            // Move a linha do pivô para a posição atual:
            if (pivot != col) {
                double[] tmp = A[pivot];
                A[pivot] = A[col];
                A[col] = tmp;
            }

            // Torna o pivô igual a 1 e divide toda a linha por seu valor:
            double diag = A[col][col];
            for (int j = col; j <= n; j++) A[col][j] /= diag;

            // Zera todos os outros elementos da coluna atual:
            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double factor = A[r][col]; // Calcula o fator de eliminação da linha.
                if (Math.abs(factor) < 1e-15) continue;

                // Subtrai a linha do pivô multiplicada pelo fator da linha atual:
                for (int j = col; j <= n; j++) A[r][j] -= factor * A[col][j];
            }
        }

        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = A[i][n];  // A solução fica na última coluna.
        return x;
    }

    // Inversão de matriz 3x3:
    public static double[][] invert3x3(double[][] m) {
        double a = m[0][0], b = m[0][1], c = m[0][2];
        double d = m[1][0], e = m[1][1], f = m[1][2];
        double g = m[2][0], h = m[2][1], i = m[2][2];

        // Encontra o determinante:
        double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (Math.abs(det) < 1e-15) throw new RuntimeException("Matriz 3x3 singular");
       
        double invdet = 1.0 / det;
        double[][] inv = new double[3][3];

        inv[0][0] = (e * i - f * h) * invdet;
        inv[0][1] = (c * h - b * i) * invdet;
        inv[0][2] = (b * f - c * e) * invdet;
        inv[1][0] = (f * g - d * i) * invdet;
        inv[1][1] = (a * i - c * g) * invdet;
        inv[1][2] = (c * d - a * f) * invdet;
        inv[2][0] = (d * h - e * g) * invdet;
        inv[2][1] = (b * g - a * h) * invdet;
        inv[2][2] = (a * e - b * d) * invdet;
        return inv;
    }

    /* FUNÇÕES DE TRANSFORMAÇÃO */

    // Escala:
    // [x'] =  [sx  0 ] [x]
    // [y']    [0   sy] [y]
    public static BufferedImage scale(BufferedImage src, double sx, double sy) {
        // sx e sy são as proporções de aumento/diminuição da escala para largura e altura, respectivamente.
        // Calcula as novas dimensões, baseadas nos fatores de escala:
        int newWidth  = (int)(src.getWidth()  * sx);
        int newHeight = (int)(src.getHeight() * sy);

        BufferedImage dst = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        // Percorre cada pixel da imagem em escala, encontrando o pixel original que corresponde ao da nova imagem.
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                // Para isso, divide cada pixel pelo fator de escala:
                double origX = x / sx;
                double origY = y / sy;

                int rgb = bilinearInterpolate(src, origX, origY);

                dst.setRGB(x, y, rgb);  // Define a cor do pixel.
            }
        }

        return dst;
    }

    // Rotação:
    // [x']   [cos θ  -sin θ] [x]
    // [y'] = [sin θ   cos θ] [y]
    public static BufferedImage rotation(BufferedImage src, double theta) {
        int w = src.getWidth();
        int h = src.getHeight();
        
        // Calcula as novas dimensões para a imagem rotacionada:
        double cos = Math.abs(Math.cos(theta));
        double sin = Math.abs(Math.sin(theta));
        int newWidth = (int) Math.ceil(w * cos + h * sin);
        int newHeight = (int) Math.ceil(w * sin + h * cos);
        
        // Centro da nova imagem:
        double cx = newWidth / 2.0;
        double cy = newHeight / 2.0;
        
        BufferedImage dst = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        
        // Centro da imagem original:
        double origCx = w / 2.0;
        double origCy = h / 2.0;
        
        cos = Math.cos(theta);
        sin = Math.sin(theta);
        
        // Percorre cada pixel da imagem rotacionada e calcula de qual pixel ele veio originalmente:
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                // Move para coordenadas relativas ao centro:
                double dx = x - cx;
                double dy = y - cy;
                
                // Rotação inversa:
                double origX = dx * cos + dy * sin;
                double origY = -dx * sin + dy * cos;
                
                // Volta para coordenadas absolutas da imagem original:
                double finalX = origX + origCx;
                double finalY = origY + origCy;
                
                int rgb = bilinearInterpolate(src, finalX, finalY);
                
                dst.setRGB(x, y, rgb);
            }
        }
        
        return dst;
    }

    // Translação:
    // [x']   [x] + [dx]
    // [y'] = [x]   [dy]
    public static BufferedImage translation(BufferedImage src, double dx, double dy) {
        int w = src.getWidth();
        int h = src.getHeight();
        
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                // Mapeamento inverso
                double origX = x - dx;
                double origY = y - dy;

                int rgb = bilinearInterpolate(src, origX, origY);

                dst.setRGB(x, y, rgb);
            }
        }

        return dst;
    }

    // Escala + Rotação + Translação (Coordenadas Homogêneas):
    // [x']   [sx*cosθ  -sx*sinθ  tx] [x]
    // [y'] = [sy*sinθ   sy*cosθ  ty] [y]
    // [w']   [   0        0       1] [1]
    public static double[][] createAffineMatrix(double sx, double sy, double theta, double tx, double ty) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        return new double[][] {
            {sx * cos, -sx * sin, tx},
            {sy * sin,  sy * cos, ty},
            {0,         0,         1}
        };
    }

    public static BufferedImage applyTransform(BufferedImage src, double[][] M) {
        int w = (int)(src.getWidth());
        int h = (int)(src.getHeight());
        
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        double[][] invM = invert3x3(M);
        
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Transformação inversa
                double[] srcCoord = multiplyMatVec(invM, new double[]{x, y, 1});
                double srcX = srcCoord[0] / srcCoord[2];
                double srcY = srcCoord[1] / srcCoord[2];
                
                int rgb = bilinearInterpolate(src, srcX, srcY);
                dst.setRGB(x, y, rgb);
            }
        }
        
        return dst;
    
    }

    // Calcula a matriz de homografia(transformação projetiva bijetiva que preserva linhas e altera ânuglos) 3x3:
    public static double[][] computeHomography(double[][] srcPts, double[][] dstPts) {
        double[][] A = new double[8][8];
        double[] b = new double[8];

        // Para cada um dos 4 pontos de correspondência, gera 2 equações:
        for (int i = 0; i < 4; i++) {
            double x = srcPts[i][0];
            double y = srcPts[i][1];
            double dstX = dstPts[i][0];
            double dstY = dstPts[i][1];
    
            int row = i * 2; // Cada ponto gera 2 linhas no sistema.

            // h00*x + h01*y + h02 + 0*h10 + 0*h11 + 0*h12 - u*x*h20 - u*y*h21 = u
            A[row][0] = x;    
            A[row][1] = y;    
            A[row][2] = 1;  
            A[row][3] = 0;  
            A[row][4] = 0;  
            A[row][5] = 0;  
            A[row][6] = -dstX * x; 
            A[row][7] = -dstX * y;
            b[row] = dstX;

            // 0*h00 + 0*h01 + 0*h02 + h10*x + h11*y + h12 - v*x*h20 - v*y*h21 = v
            A[row + 1][0] = 0;  
            A[row + 1][1] = 0;  
            A[row + 1][2] = 0;  
            A[row + 1][3] = x;  
            A[row + 1][4] = y;  
            A[row + 1][5] = 1;  
            A[row + 1][6] = -dstY * x; 
            A[row + 1][7] = -dstY * y;
            b[row + 1] = dstY;
        }

        double[] h = solveLinearSystem(A, b); 
        double[][] H = new double[3][3];
        H[0][0] = h[0]; H[0][1] = h[1]; H[0][2] = h[2];
        H[1][0] = h[3]; H[1][1] = h[4]; H[1][2] = h[5];
        H[2][0] = h[6]; H[2][1] = h[7]; H[2][2] = 1.0;
        return H;
    }

    // Aplica a homografia na imagem:
    public static BufferedImage applyHomography(BufferedImage src, double[][] H, int dstW, int dstH) {
        double[][] corners = {
            {0, 0}, {src.getWidth()-1, 0}, 
            {src.getWidth()-1, src.getHeight()-1}, {0, src.getHeight()-1}
        };
        
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (double[] corner : corners) {
            double[] p = multiplyMatVec(H, new double[]{corner[0], corner[1], 1});
            double x = p[0] / p[2];
            double y = p[1] / p[2];
            
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        
        int newWidth = (int) Math.ceil(maxX - minX);
        int newHeight = (int) Math.ceil(maxY - minY);
        
        BufferedImage dst = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        double[][] invH = invert3x3(H);

        // Percorre todos os pixels da nova imagem:
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                // Ajusta as coordenadas considerando o deslocamento (minX, minY):
                double worldX = x + minX;
                double worldY = y + minY;
                
                // Aplica transformação inversa:
                double[] p = multiplyMatVec(invH, new double[]{worldX, worldY, 1});
                double px = p[0] / p[2];
                double py = p[1] / p[2];
                
                int rgb = bilinearInterpolate(src, px, py);
                dst.setRGB(x, y, rgb);
               
            }
        }
        return dst;
    }
}
