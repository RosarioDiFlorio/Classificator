package eu.innovation.engineering.test;

import java.util.ArrayList;
import java.util.List;

import eu.innovation.engineering.LSA.keywordExtractor.LSACosineKeywordExtraction;
import eu.innovation.engineering.LSA.keywordExtractor.LSAKeywordExtractor;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;

public class TesterChuncker {

  public static void main(String[] args) throws Exception{

    
    String test = "Development of the BASS rake acoustic current sensor : measuring velocity in the continental shelf wave bottom boundary layer"
        +"Surface swell over the continental shelf generates a sheet of oscillatory shear flow at"
        +"the base of the water column, the continental shelf wave bottom boundary layer. The"
        +"short periods of surface swell sharply limit the thickness of the wave boundary layer,"
        +"confining it to a thin region below an oscillatory, but essentially irrotational, core. For"
        +"a wide range of shelf conditions, the vertical extent of the wave boundary layer does"
        +"not exceed 2.5 cm and is commonly less. The extreme narrowness of this boundary"
        +"layer is responsible for high levels of bottom stress and turbulent dissipation. Even in"
        +"relatively mild sea states, the wave induced bottom shear stress can be sufficient to"
        +"initiate sediment motion. The wave bottom boundary layer plays an important role"
        +"in the processes of sediment entrainment and transport on the continental margins."
        +"This thesis documents the development, testing, and field use of a new instrument,"
        +"the BASS Rake, designed to measure velocity profiles in the wave boundary layer."
        +"The mechanical design supports multiple measurement levels with millimeter vertical"
        +"spacing. The mechanical design is integrated with an electronic interface designed to"
        +"permit flexible acquisition of a suite of horizontal and vertical velocity measurements"
        +"without sacrificing the electronic characteristics necessary for high measurement accuracy. The effects of velocity averaging over the sample volume are calculated with"
        +"a model of acoustic propagation in a scattering medium appropriate to the scales of"
        +"a single differential travel time axis. A simpler parametric model of the averaging"
        +"process is then developed and used to specify the transducer characteristics necessary"
        +"to image the wave boundary layer on the continental shelf."
        +"A flow distortion model for the sensor head is developed and the empirical determinations"
        +"of the Reynolds number, Keulegan-Carpenter number, and angular dependencies"
        +"of the sensor response for the laboratory and field prototypes is presented."
        +"The calibrated sensor response of the laboratory prototype is tested against concurrent"
        +"LDV measurements over a natural sand bed in a flume. The single measurement"
        +"accuracy of the BASS Rake is higher than that of the LDV and the multiple sample volumes confer other advantages. For example, the ability of the BASS Rake to image"
        +"vertically coherent turbulent instabilities, invisible to the LDV, is demonstrated."
        +"Selected data from a twenty-four day field deployment outside the surf zone of a local"
        +"beach are presented and analyzed. The data reveal regular reworking of the sand"
        +"bed, the generation and modification of sand ripples, and strong tidal modulation of"
        +"the current and wave velocities on semi-diurnal, diurnal, and spring eap time scales."
        +"The data set is unique in containing concurrent velocity time series, of several weeks"
        +"duration, with coverage from 1 cm to 20 cm above the bottom.Submitted in partial fulfillment of the requirements for the degree of Doctor of Philosophy at the Massachusetts Institute of Technology and the Woods Hole Oceanographic Institution June 1997ONR has contributed"
        +"substantial financial support to this research and to my graduate education"
        +"through AASERT funding under ONR Grant N00014-93-1-1140. NSF supported the purchase of"
        +"hardware and services under NSF Grant OCE-9314357";
    LSACosineKeywordExtraction kex = new LSACosineKeywordExtraction("","");


    List<String> testList = new ArrayList<>();
    testList.add(test);

    /*
    List<String> toCompare = new ArrayList<>();

    toCompare.addAll(kex.readGlossay("data/Glossary_of_biology.txt"));
    toCompare.addAll(kex.readGlossay("data/Glossary_of_chemistry_terms.txt"));
    toCompare.addAll(kex.readGlossay("data/mathematics.txt"));

    //toCompare.add("science");
    System.out.println(kex.extractKeywordsFromTexts(testList, toCompare,5));
    */
    long startTime = 0;
    KeywordExtractor ke = new InnenExtractor("");
    KeywordExtractor lsake = new LSAKeywordExtractor("");
    
    startTime = System.currentTimeMillis();
    ke.extractKeywordsFromTexts(testList, 4);
    System.out.println(System.currentTimeMillis() - startTime);
    
    
    startTime = System.currentTimeMillis();
    lsake.extractKeywordsFromTexts(testList, 4);
    System.out.println(System.currentTimeMillis() - startTime);

    
   
    /*
    StanfordnlpAnalyzer analyzer = new StanfordnlpAnalyzer();
    StanfordLemmatizer lemmStan = new StanfordLemmatizer();
    Lemmatizer lemm = new Lemmatizer();
    long startTime = 0;
    




    startTime = System.currentTimeMillis();
    System.out.println(kex.createSentencesFromText(test));



    startTime = System.currentTimeMillis();
    lemm.lemmatize(test);
    System.out.println(System.currentTimeMillis() - startTime);

    /*
    startTime = System.currentTimeMillis(); 
    List<String> sentences = analyzer.detectSentences(test, ISO_639_1_LanguageCode.ENGLISH);
    System.out.println(System.currentTimeMillis() - startTime);

    startTime = System.currentTimeMillis();    
    System.out.println(lemmStan.lemmatizeTerm(test, "", ISO_639_1_LanguageCode.ENGLISH));
    System.out.println(System.currentTimeMillis() - startTime);
     */

  }




}
