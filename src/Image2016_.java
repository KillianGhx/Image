import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;

/**
 * Rien a completer, juste a tester. (Encore faut-il que la methode Outils.convoluer(...) fonctionne.) Le masque de convolution est le suivant: | 0 1 0 | L4 = | 1 -4 1 | | 0 1 0 |
 */
public class Image2016_ implements PlugInFilter {

	public void run(ImageProcessor ip) {
		// Sobel
		ImagePlus imp = this.afficheMatrice(ip, "Sobel", true);

		// Composantes Connexes 
		HashMap<Integer, ArrayList<Integer[]>> composantes = composantes(ip);
		
		// TODO Ajouter l'étude des composantes connexes

		new ImageWindow(imp);
	}

	public int setup(String args, ImagePlus imp) {
		return NO_CHANGES + DOES_8G;
	}

	/**
	 * Cherches les composantes connexes d'une image
	 * 
	 * @param ip
	 *            L'image à étudier
	 */
	public static HashMap<Integer, ArrayList<Integer[]>> composantes(ImageProcessor ip) {

		// Numéro de la composante -> liste des pixels qu'elle inclut
		HashMap<Integer, ArrayList<Integer[]>> composantes = new HashMap<Integer, ArrayList<Integer[]>>();
		int couleur = 255, height = ip.getHeight(), width = ip.getWidth();

		// Parcours des pixels
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){

				// Si le pixel est de la couleur du contour et qu'il n'est pas dans les composantes
				if(ip.getPixel(x, y) == couleur && !isVisited(composantes, x, y)){
					int composanteKey = -1;
					ArrayList<Integer[]> toAdd = new ArrayList<Integer[]>();
					Integer[] pixel = {x, y};
					toAdd.add(pixel);

					// TODO Gérer la rencontre de plusieurs composantes en (x,y)
					// On regarde les pixels adjacents pour les ajouter à une éventuelle composante
					// et pour ne pas en créer une de trop
					for(int i=x-1;i<x+2;x++){
						for(int j=y-1;j<y+2;y++){
							if(ip.getPixel(i, j) == couleur){
								if(isVisited(composantes, i, j)){
									// S'il y a déjà une autre composante d'identifiée c'est que plusieurs se rejoignent : une fusion s'impose
									if(composanteKey > -1 && composanteKey != getComposanteKey(composantes, i, j)){
										int composanteKey2 = getComposanteKey(composantes, i, j);
										ArrayList<Integer[]> insertInto = composantes.get(composanteKey);
										Iterator<Integer[]> iterator = composantes.get(composanteKey2).iterator();
										
										while(iterator.hasNext()){
											insertInto.add(iterator.next());
										}
										
										composantes.put(composanteKey, insertInto);
										int k=composanteKey2;
										
										for(;k<composantes.size()-1;k++){
											composantes.put(k, composantes.get(k+1));
										}
										
										composantes.remove(k);
									}else{
										composanteKey = getComposanteKey(composantes, i, j);
									}
								}else{
									Integer[] pixel2 = {i, j};
									toAdd.add(pixel2);
								}
							}
						}
					}
					
					Iterator<Integer[]> iterator = toAdd.iterator();
					ArrayList<Integer[]> insertInto = composantes.get(composanteKey);
					
					// On ajoute les pixels découverts dans la composante correspondantes
					while(iterator.hasNext()){
						insertInto.add(iterator.next());
					}
					
					composantes.put(composanteKey, insertInto);
				}
			}
		}
		
		return composantes;
	}

	/**
	 * Retourne la clé dans le hashmap composantes de la composante contenant le pixel aux coordonnées x et y
	 * 
	 * @param composantes
	 * @param x
	 * @param y
	 * @return int key le numéro de la composante
	 */
	public static int getComposanteKey(HashMap<Integer, ArrayList<Integer[]>> composantes, int x, int y) {
		ArrayList<Integer[]> pixel = new ArrayList<Integer[]>();
		Integer[] coordinates = {x, y};
		int key = -1, i = 0;

		pixel.add(coordinates);

		while(key < 0 && i < composantes.size()){
			if(composantes.get(i).containsAll(pixel))
				key = i;
			i++;
		}

		return key;
	}

	/**
	 * Vérifie que le pixel aux coordonnées x et y ne soient pas déjà présent dans composantes
	 * 
	 * @param composantes
	 * @param x
	 * @param y
	 * @return boolean visited true s'il est présent et false autrement
	 */
	public static boolean isVisited(HashMap<Integer, ArrayList<Integer[]>> composantes, int x, int y) {
		boolean visited = false;
		ArrayList<Integer[]> pixel = new ArrayList<Integer[]>();
		Integer[] coordinates = {x, y};
		int i = 0;

		pixel.add(coordinates);

		while(i < composantes.size() && !visited){
			if(composantes.get(i).containsAll(pixel))
				visited = true;
			i++;
		}

		return visited;
	}

	/**
	 * Effectue la convolution de l'image 'ip' avec un masque carre. Le resultat d'un produit de convolution n'est pas forcement dans le meme domaine de definition que l'image d'origine. C'est pourquoi le resultat est stocke dans une matrice de nombres reels.
	 * 
	 * @param ip
	 *            L'image a convoluer.
	 * @param masque
	 *            Le masque de convolution.
	 * @return La matrice resultat.
	 */
	public static double[][] convoluer(ImageProcessor ip, Masque masque) {
		/**
		 * A faire: effectuer la convolution. Reflechir a la question des bords.
		 */
		// resultat: la matrice dans laquelle sera stocke le resultat de la convolution.
		double[][] resultat = new double[ip.getWidth()][ip.getHeight()];

		int rayon = masque.getRayon();
		for(int i = rayon; i < ip.getHeight() - rayon; i++){
			for(int j = rayon; j < ip.getWidth() - rayon; j++){
				resultat[j][i] = 0;
				// Parcours du masque de convolution
				for(int v = -rayon; v <= rayon; v++){
					for(int u = -rayon; u <= rayon; u++){
						resultat[j][i] += masque.get(u, v) * ip.getPixel(j - u, i - v);
					}
				}
			}
		}
		/**
		 * Fin de la partie a completer
		 */
		return resultat;
	}

	/**
	 * Affiche une matrice de nombres reels dans une nouvelle fenetre. Comme les elements de cette matrice ne sont pas forcement dans le domaine [0..255], on a le choix entre: 1) normaliser: c'est-a-dire faire une mise a l'echelle de maniere a ce que la valeur la plus faible soit 0 et la valeur la plus haute 255. (voir TP1: etirement d'histogramme). 2) ne pas normaliser: tous les elements dont la valeur est inferieure a 0 sont fixes a 0 et tous les elements dont la valeur est superieure a 255 sont fixes a 255.
	 * 
	 * @param mat
	 *            La matrice a afficher.
	 * @param titre
	 *            Le titre de la fenetre.
	 * @param normaliser
	 *            Faut-il normaliser ?
	 */
	private ImagePlus afficheMatrice(ImageProcessor ip, String titre, boolean normaliser) {
		// mat: la matrice a remplir
		double[][] mat;

		Masque sobelX = new Masque(1);
		sobelX.put(-1, -1, -1);
		sobelX.put(1, -1, 1);
		sobelX.put(-1, 0, -2);
		sobelX.put(1, 0, 2);
		sobelX.put(-1, 1, -1);
		sobelX.put(1, 1, 1);
		double[][] matX = convoluer(ip, sobelX);

		Masque sobelY = new Masque(1);
		sobelY.put(-1, -1, -1);
		sobelY.put(0, -1, -2);
		sobelY.put(1, -1, -1);
		sobelY.put(-1, 1, 1);
		sobelY.put(0, 1, 2);
		sobelY.put(1, 1, 1);
		double[][] matY = convoluer(ip, sobelY);

		mat = new double[ip.getWidth()][ip.getHeight()];
		for(int i = 0; i < ip.getHeight(); i++){
			for(int j = 0; j < ip.getWidth(); j++){
				mat[j][i] = Math.sqrt(Math.pow(matX[j][i], 2) + Math.pow(matY[j][i], 2));
			}
		}

		ImagePlus imp = NewImage.createByteImage(titre, mat.length, mat[0].length, 1, NewImage.FILL_BLACK);
		ImageProcessor ip2 = imp.getProcessor();

		if(normaliser){
			double max = mat[0][0];
			double min = mat[0][0];
			for(int y = 0; y < mat[0].length; y++){
				for(int x = 0; x < mat.length; x++){
					if(mat[x][y] > max)
						max = mat[x][y];
					if(mat[x][y] < min)
						min = mat[x][y];
				}
			}

			if(min != max){
				for(int y = 0; y < mat[0].length; y++){
					for(int x = 0; x < mat.length; x++){
						ip2.putPixel(x, y, (int) ((255 * (mat[x][y] - min)) / (max - min)));
					}
				}
			}
		}

		else{
			for(int y = 0; y < mat[0].length; y++){
				for(int x = 0; x < mat.length; x++){
					int p = (int) Math.min(mat[x][y], 255);
					p = Math.max(p, 0);
					ip2.putPixel(x, y, p);
				}
			}
		}

		return imp;
	}

	public class Masque {

		private double[] contenu;
		private int rayon;
		private int largeur;

		/**
		 * Cree un nouveau masque de convolution. C'est un carre de cote (2 * rayon + 1). Tous les elements sont a zero.
		 * 
		 * @param rayon
		 *            Rayon du masque de convolution.
		 */
		public Masque(int rayon) {
			this(rayon, 0);
		}

		/**
		 * Cree un nouveau masque de convolution. C'est un carre de cote (2 * rayon + 1). Tous les elements sont a 'valeurParDefaut'.
		 * 
		 * @param rayon
		 *            Rayon du masque de convolution.
		 * @param valeurParDefaut
		 *            Valeur a mettre dans chaque element.
		 */
		public Masque(int rayon, double valeurParDefaut) {
			if(rayon < 1){
				throw new IllegalArgumentException("Le rayon doit etre >= 1");
			}

			this.rayon = rayon;
			largeur = 2 * rayon + 1;
			contenu = new double[largeur * largeur];

			for(int i = 0; i < largeur * largeur; i++){
				contenu[i] = valeurParDefaut;
			}
		}

		/**
		 * Renvoie le rayon (demie-largeur) du masque.
		 * 
		 * @return Le rayon.
		 */
		public int getRayon() {
			return rayon;
		}

		/**
		 * Renvoie la largeur du masque (cote du carre).
		 * 
		 * @return La largeur.
		 */
		public int getLargeur() {
			return largeur;
		}

		/**
		 * Remplit le masque avec la valeur passee en argument.
		 * 
		 * @param valeur
		 *            Valeur a stocker dans chaque element.
		 */
		public void remplirAvec(double valeur) {
			for(int i = 0; i < largeur * largeur; i++){
				contenu[i] = valeur;
			}
		}

		/**
		 * Renvoie un element du masque.
		 * 
		 * @param x
		 *            Abscisse de l'element.
		 * @param y
		 *            Ordonnee de l'element.
		 * @return La valeur contenue dans l'element de coordonnees (x,y).
		 */
		public double get(int x, int y) {
			return contenu[(y + rayon) * largeur + x + rayon];
		}

		/**
		 * Modifie un element du masque.
		 * 
		 * @param x
		 *            Abscisse de l'element.
		 * @param y
		 *            Ordonnee de l'element.
		 * @param valeur
		 *            Valeur a inscrire dans l'element de coordonnees (x,y).
		 */
		public void put(int x, int y, double valeur) {
			contenu[(y + rayon) * largeur + x + rayon] = valeur;
		}

	}
}
