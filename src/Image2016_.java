import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;

public class Image2016_ implements PlugInFilter {

	public void run(ImageProcessor ip) {

		// Sobel
		ImagePlus imp = this.afficheMatrice(ip, "Sobel", true);
		ImageProcessor ip2 = imp.getProcessor();

		// Composantes Connexes
		// TODO Pourquoi les composantes ne sont-elles pas enregistrées ?
		HashMap<Integer, ArrayList<int[]>> composantes = getComposantes(ip2);

		// Cadres
		appliqueCadres(cadres(composantes, ip2), ip2);
		// TODO Ajouter l'étude des composantes connexes

		new ImageWindow(imp);
	}

	public int setup(String args, ImagePlus imp) {
		return NO_CHANGES + DOES_8G;
	}

	/**
	 * Applique les cadres trouvés pour chaque composante connexe afin de montrer que la reconnaissance de composantes fonctionne relativement correctement
	 * 
	 * @param cadres
	 *            Les cadres à appliquer
	 * @param ip
	 */
	public static void appliqueCadres(HashMap<Integer, int[]> cadres, ImageProcessor ip) {
		int couleur = 255;

		for(int i = 0; i < cadres.size(); i++){
			// Constante des index du tableau des extrêmeités des cadres
			int minX = 0, minY = 2, maxX = 3, maxY = 4;
			int[] extremes = cadres.get(i);

			// Coté gauche
			for(int x = extremes[minX] - 100; x <= extremes[minX]; x++){
				ip.drawLine(x, extremes[minY] - 100, x, extremes[maxY] + 100);
			}

			// Coté droit
			for(int x = extremes[maxX]; x <= extremes[maxX] + 100; x++){
				ip.drawLine(x, extremes[minY] - 100, x, extremes[maxY] + 100);
			}

			// Haut
			for(int y = extremes[maxY]; y < extremes[maxY] + 100; y++){
				ip.drawLine(extremes[minX], y, extremes[maxX], y);
			}

			// Bas
			for(int y = extremes[minY]; y < extremes[minY] - 100; y++){
				ip.drawLine(extremes[minX], y, extremes[maxX], y);
			}
		}

		//TODO A retirer quand le débuggage des composantes sera fini
		if(cadres.size() > 0)
			couleur = 255;
		else
			couleur = 0;
		int width = ip.getWidth(), height = ip.getHeight();

		for(int i = 0; i < width / 2; i++){
			for(int j = 0; j < height; j++)
				ip.set(i, j, couleur);
		}
	}

	/**
	 * Créé et retourne des cadres rouges à afficher pour chaque composante conne de HashMap composantes
	 * 
	 * @param composantes
	 *            Les composantes connexes
	 * @return cadres Les extrêmités horizontales et verticales de chaques composantes sous la forme suivante
	 * 
	 *         HashMap( index => Integer[minX, minY, maxX, maxY] )
	 */
	public static HashMap<Integer, int[]> cadres(HashMap<Integer, ArrayList<int[]>> composantes, ImageProcessor ip) {
		HashMap<Integer, int[]> cadres = new HashMap<Integer, int[]>();

		// Pour chaque composante trouvée
		for(int i = 1; i < composantes.size(); i++){
			int minX = ip.getWidth() - 1, minY = ip.getHeight() - 1, maxX = 0, maxY = 0;
			// Coordonnées des pixels compris dans la composantes
			Iterator<int[]> iterator = composantes.get(i).iterator();

			// Pour chaque pixel, on cherche les extrémités verticales et horizontales
			while(iterator.hasNext()){
				int[] next = iterator.next();

				minX = min(next[0], minX);
				maxX = max(next[0], maxX);
				minY = min(next[1], minY);
				maxY = max(next[1], maxY);
			}
			int[] extremes = {minX, minY, maxX, maxY};

			cadres.put(i, extremes);
		}

		return cadres;
	}

	/**
	 * Cherches les composantes connexes d'une image
	 * 
	 * @param ip
	 *            L'image à étudier
	 * @return composantes Les composantes connexes trouvées sous la forme suivante HashMap( index => ArrayList(Integer[x, y]) )
	 * 
	 *         index étant le numéro de la composante partant de 0 et s'incrémentant de 1 à chaque composante ajoutée x et y étant les coordonnées respectivement verticales et horizontales d'un pixel
	 */
	public static HashMap<Integer, ArrayList<int[]>> getComposantes(ImageProcessor ip) {

		// Numéro de la composante -> liste des pixels qu'elle inclut
		HashMap<Integer, ArrayList<int[]>> composantes = new HashMap<Integer, ArrayList<int[]>>();
		int couleur = 90, height = ip.getHeight(), width = ip.getWidth();

		// Parcours des pixels
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){

				// Si le pixel est de la couleur du contour et qu'il n'est pas dans les composantes
				if(ip.getPixel(x, y) > couleur && !isVisited(composantes, x, y)){
					int composanteKey = -1;
					ArrayList<int[]> toAdd = new ArrayList<int[]>();
					int[] pixel = {x, y};
					toAdd.add(pixel);

					/*
					 * On vérifie si les pixels autour sont dans les bonnes couleurs et s'ils font déjà parti d'une composante Voisin dans une composante ? Si le pixel sélectionné n'a pas de composante on le place dans celle-ci autrement on ajoute celle du voisin dans la composante provisoire du pixel sélectionné Voisin pas dans une composante ? On passe au suivant
					 */
					for(int i = x - 1; i < x + 2; x++){
						for(int j = y - 1; j < y + 2; y++){

							// Le pixel voisin est dans les bonnes couleurs
							if(ip.getPixel(i, j) > couleur){

								// Si le voisin du pixel sélectionné fait parti d'une composante
								if(isVisited(composantes, i, j)){
									// Si le pixel sélectionné ne fait pas parti de composante, on le place dans celle du voisin
									if(composanteKey == -1){
										composanteKey = getComposanteKey(composantes, i, j);
									}
									// Si le pixel sélectionné a été ajouté à une composante et qu'un voisin fait déjà parti
									// d'une autre composante c'est que plusieurs se rejoignent : une fusion s'impose
									else if(composanteKey != getComposanteKey(composantes, i, j)){
										int composanteKey2 = getComposanteKey(composantes, i, j);
										ArrayList<int[]> insertInto = composantes.get(composanteKey);
										Iterator<int[]> iterator = composantes.get(composanteKey2).iterator();

										while(iterator.hasNext()){
											insertInto.add(iterator.next());
										}

										composantes.put(composanteKey, insertInto);
										int k = composanteKey2;

										for(; k < composantes.size() - 1; k++){
											composantes.put(k, composantes.get(k + 1));
										}

										composantes.remove(k);
									}
								}else{ // Il n'en fait pas parti, donc on le rajoute à la composante en devenir
									int[] pixel2 = {i, j};
									toAdd.add(pixel2);
								}
							}
						}
					}
					
					// Si aucune composante n'a été sélectionnée pendant l'exploration des voisins, on place la composante provisoire à la fin
					if(composanteKey == -1)
						composanteKey = composantes.size();

					Iterator<int[]> iterator = toAdd.iterator();
					ArrayList<int[]> insertInto = composantes.get(composanteKey);

					 if(insertInto == null)
						insertInto = new ArrayList<int[]>();

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
	public static int getComposanteKey(HashMap<Integer, ArrayList<int[]>> composantes, int x, int y) {
		ArrayList<int[]> pixel = new ArrayList<int[]>();
		int[] coordinates = {x, y};
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
	public static boolean isVisited(HashMap<Integer, ArrayList<int[]>> composantes, int x, int y) {
		boolean visited = false;
		int i = 0;

		while(i < composantes.size() && !visited){
			Iterator<int[]> iterator = composantes.get(i).iterator();

			while(iterator.hasNext() && !visited){
				int[] next = iterator.next();
				if(next[0] == x && next[1] == y)
					visited = true;
			}
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

	/**
	 * Retourne le plus petit de a et b
	 * 
	 * @param a
	 * @param b
	 * @return le plus petit nombre de a et b
	 */
	public static int min(int a, int b) {
		if(a > b)
			return b;
		else
			return a;
	}

	/**
	 * Retourne le plus grand de a et b
	 * 
	 * @param a
	 * @param b
	 * @return le plus grand nombre de a et b
	 */
	public static int max(int a, int b) {
		if(a < b)
			return b;
		else
			return a;
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
