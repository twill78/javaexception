package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;
    
	// Liste pour sauvegarder tous les matricules de managers valides du fichier CSV
	private List<String> matsManager = new ArrayList<String>();

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<Employe>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        
        // Vérifie si le fichier existe
        try {
        	readFile(fileName);
        } catch(IOException exception) {
        	throw new IOException("Fichier introuvable :" + fileName);
        }
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) throws Exception {
        Stream<String> stream;
        stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        
        Integer i = 0;
        
        //TODO
        for(String ligne : stream.collect(Collectors.toList())) {
        	i++;
        	try {
        		processLine(ligne);
        	} catch (BatchException e) {
        		System.out.println("Ligne " + i + " : " + e.getMessage() + "=> " + ligne);
        	}
        	
        	// Enregistre tous les matricules de managers valides
        	String[] tab2 = ligne.split(",");
        	if(tab2[0].matches(REGEX_MATRICULE_MANAGER)) {
        		this.matsManager.add(tab2[0]);
        	}
        	
        }

        System.out.println("\n----------------------------------------------");
        System.out.println("LISTE DES EMPLOYES QUI VONT ETRE ENREGISTRES :");
        printEmployes();
        System.out.println("----------------------------------------------\n");
        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        //TODO
    	String[] tab = ligne.split(",");
    	
    	// Vérifie si la ligne est incomplète
    	if (tab.length < 5) {
    		throw new BatchException("La ligne ne comporte pas assez d'éléments : " + tab.length + " ");
    	}
    	
    	// Déclare le format de date attendu
    	SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");
    	formatDate.setLenient(false);
    	
    	// Vérifie si la première lettre du matricule est correcte
    	if(!ligne.matches("^[MCT]{1}.*")) {
    		throw new BatchException("Type d'employé inconnu : " + ligne.charAt(0) + " ");
    	}
    	
    	// Vérifie si le format du matricule est correct
    	if(!tab[0].matches(REGEX_MATRICULE)) {
    		throw new BatchException("la chaîne " + tab[0] + " ne respecte pas l'expression régulière ");
    	}
    	
    	// Vérifie si le nb de champs pour un manager est correct
    	if(ligne.matches("^[M]{1}.*") && tab.length != NB_CHAMPS_MANAGER) {
    		throw new BatchException("La ligne manager ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + tab.length + " ");
    	}
    	
    	// Vérifie si le format de date est correct
    	try {
    		LocalDate d = new LocalDate(formatDate.parse(tab[3]));
    	} catch (ParseException e){
    		throw new BatchException(tab[3] + " ne respecte pas le format de date dd/MM/yyyy ");
    	}
    	
    	// Vérifie si le salaire est de type double
    	try {
            double d = Double.parseDouble(tab[4]);
        } catch (NumberFormatException e) {
        	throw new BatchException(tab[4] + " n'est pas un nombre valide pour un salaire ");
        }
    	
    	// Vérifie si le nb de champs pour un commercial est correct
    	if(ligne.matches("^[C]{1}.*") && tab.length != NB_CHAMPS_COMMERCIAL) {
    		throw new BatchException("La ligne commercial ne contient pas " + NB_CHAMPS_COMMERCIAL + " éléments mais " + tab.length + " ");
    	}
    	
    	// Vérifie si le CA d'un commercial est de type double
    	if(ligne.matches("^[C]{1}.*")) {
    		try {
	            double d = Double.parseDouble(tab[5]);
	        } catch (NumberFormatException e) {
	        	throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + tab[5] + " ");
	        }
    	}

    	// Vérifie si la performance d'un commercial est de type integer
    	if(ligne.matches("^[C]{1}.*")) {
    		try {
	            Integer perf = Integer.parseInt(tab[6]);
	        } catch (NumberFormatException e) {
	        	throw new BatchException("La performance du commercial est incorrecte : " + tab[6] + " ");
	        }
    	}
    	
    	// Vérifie si le nb de champs pour un technicien est correct
    	if(ligne.matches("^[T]{1}.*") && tab.length != NB_CHAMPS_TECHNICIEN) {
    		throw new BatchException("La ligne technicien ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + tab.length + " ");
    	}
    	
    	// Vérifie si le grade d'un technicien est de type integer
    	if(ligne.matches("^[T]{1}.*")) {
    		try {
	            Integer grade = Integer.parseInt(tab[5]);
	        } catch (NumberFormatException e) {
	        	throw new BatchException("Le grade du technicien est incorrect : " + tab[5] + " ");
	        }
    	}
    	
    	// Vérifie si le grade d'un technicien est compris entre 1 et 5
    	if(ligne.matches("^[T]{1}.*") && (Integer.parseInt(tab[5]) < 1 || Integer.parseInt(tab[5]) > 5)) {
    		throw new BatchException("Le grade doit être compris entre 1 et 5 : " + tab[5] + " ");
    	}
    	
    	// Vérifie si le matricule du manager d'un technicien est valide
    	if(ligne.matches("^[T]{1}.*") && !tab[6].matches(REGEX_MATRICULE_MANAGER)) {
    		throw new BatchException("la chaîne " + tab[6] + " ne respecte pas l'expression régulière ^M[0-9]{5}$ ");
    	}
    	
    	// Vérifie si le matricule du manager du technicien est déjà présent en base ou dans le CSV
    	if(ligne.matches("^[T]{1}.*") && tab[6].matches(REGEX_MATRICULE_MANAGER)) {
    		String mat = tab[6];
    		Employe t = employeRepository.findByMatricule(mat);
    		
    		for(String matManager : matsManager) {
    			if(t == null && !mat.equals(matManager)) {
            		throw new BatchException("Le manager de matricule " + mat + " n'a pas été trouvé dans le fichier ou en base de données ");
            	}
	    	}
    	}
    	
    	// Création d'un commercial
    	if(ligne.matches("^[C]{1}.*")) {
    		processCommercial(ligne);
    	}
    	
    	// Création d'un manager
    	if(ligne.matches("^[M]{1}.*")) {
    		processManager(ligne);
    	}
    	
    	// Création d'un technicien
    	if(ligne.matches("^[T]{1}.*")) {
    		processTechnicien(ligne);
    	}
    	
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        //TODO
    	String[] tabC = ligneCommercial.split(",");
    	Commercial c = new Commercial();
    	
    	c.setNom(tabC[1]);
    	c.setPrenom(tabC[2]);
    	c.setMatricule(tabC[0]);
    	
    	SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");
    	formatDate.setLenient(false);
    	try {
    		LocalDate d = new LocalDate(formatDate.parse(tabC[3]));
    		c.setDateEmbauche(d);
    	} catch (Exception e) {
    		throw new BatchException(tabC[3] + " ne respecte pas le format de date dd/MM/yyyy ");
		}
    	
    	c.setSalaire(Double.parseDouble(tabC[4]));
    	c.setCaAnnuel(Double.parseDouble(tabC[5]));
    	c.setPerformance(Integer.parseInt(tabC[6]));
    	
    	this.employes.add(c);
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
    	String[] tabM = ligneManager.split(",");
    	Manager m = new Manager();
    	
    	m.setNom(tabM[1]);
    	m.setPrenom(tabM[2]);
    	m.setMatricule(tabM[0]);
    	
    	SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");
    	formatDate.setLenient(false);
    	try {
    		LocalDate d = new LocalDate(formatDate.parse(tabM[3]));
    		m.setDateEmbauche(d);
    	} catch (Exception e) {
    		throw new BatchException(tabM[3] + " ne respecte pas le format de date dd/MM/yyyy ");
		}
    	
    	m.setSalaire(Double.parseDouble(tabM[4]));
    	m.setEquipe(null);
    	
    	this.employes.add(m);
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
    	String[] tabT = ligneTechnicien.split(",");
    	Technicien t = new Technicien();
    	
    	t.setNom(tabT[1]);
    	t.setPrenom(tabT[2]);
    	t.setMatricule(tabT[0]);
    	
    	SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");
    	formatDate.setLenient(false);
    	try {
    		LocalDate d = new LocalDate(formatDate.parse(tabT[3]));
    		t.setDateEmbauche(d);
    	} catch (Exception e) {
    		throw new BatchException(tabT[3] + " ne respecte pas le format de date dd/MM/yyyy ");
		}
    	
    	try {
			t.setGrade(Integer.parseInt(tabT[5]));	
		} catch (NumberFormatException | TechnicienException e) {
			throw new BatchException("Le grade du technicien est incorrect : " + tabT[5] + " ");
		}
    	
    	t.setSalaire(Double.parseDouble(tabT[4]));
    	
    	this.employes.add(t);
    }
    
    // Méthode pour afficher la liste des employés validés
    private void printEmployes() {
    	System.out.println(employes.toString());
    }

}
