package eu.innovation.engineering.test;

import java.util.ArrayList;
import java.util.List;

import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;

public class TesterChuncker {

  public static void main(String[] args) throws Exception{
    String test= "Boundary Characterization of a Smooth Domain with Non-Compact Automorphism Group"+
        "One of the most important problems in the field of several complex variables is the Greene-Krantz conjecture: <bold>Conjecture<bold/>  Let D be a smoothly bounded domain in C<super>n<super/> with non-compact automorphism group. Then the boundary of D is of finite type at any boundary orbit accumulation point. The purpose of this dissertation is to prove a result that supports the truthfulness of this conjecture: <bold>Theorem<bold/> Let D be a smoothly bounded convex domain in C<super>n<super/>. Suppose there exists a point p in D and a sequence  of automorphisms of D, f <sub>j<sub/>, such that f <sub>j<sub/>(p) &rarr q in the boundary of D non-tangentially. Furthermore, suppose Condition LTW holds. Then, the boundary of D is variety-free at q.";

    String test2 = "Alternatively activated dendritic cells regulate CD4+ T-cell polarization in vitro and in vivo"+
        "Interleukin-4 is a cytokine widely known for its role in CD4(+) T cell polarization and its ability to alternatively activate macrophage populations. In contrast, the impact of IL-4 on the activation and function of dendritic cells (DCs) is poorly understood. We report here that DCs respond to IL-4 both in vitro and in vivo by expression of multiple alternative activation markers with a different expression pattern to that of macrophages. We further demonstrate a central role for DC IL-4Rα expression in the optimal induction of IFNγ responses in vivo in both Th1 and Th2 settings, through a feedback loop in which IL-4 promotes DC secretion of IL-12. Finally, we reveal a central role for RELMα during T-cell priming, establishing that its expression by DCs is critical for optimal IL-10 and IL-13 promotion in vitro and in vivo. Together, these data highlight the significant impact that IL-4 and RELMα can have on DC activation and function in the context of either bacterial or helminth pathogens";
    String test3 = "Atomistic Simulations of Calcium Uranyl(VI) Carbonate Adsorption on Calcite and Stepped-Calcite Surfaces"+
        "Adsorption of actinyl ions onto mineral surfaces is one of the main mechanisms that control the migration of these ions in environmental systems. Here, we present computational classical molecular dynamics (MD) simulations to investigate the behavior of U(VI) in contact with different calcite surfaces. The calcium-uranyl-carbonate [Ca2UO2(CO3)(3)] species is shown to display both inner and outer-sphere adsorption to the flat {10 (1) over bar4} and the stepped {31 (4) over bar8} and {3 (12) over bar 16} planes of calcite. Free energy calculations, using the umbrella sampling method, are employed to simulate adsorption paths of the same uranyl species on the different calcite surfaces under aqueous condition. Outer-sphere adsorption is found to dominate over inner-sphere adsorption because of the high free energy barrier of removing a uranyl-carbonate interaction and replacing it with a new uranyl-surface interaction. An important bin monolayer between the surface and the sorbed complex. From the free complex was also found to adsorb preferentially on the acute-stepped ding mode is proposed involving a single vicinal water energy profiles of the different Calcite surfaces, the uranyl {31 (4) over bar8} face of calcite, in agreement with experiment";
    String test4 = "Epidermal cytokines and skin sensitization hazard."+
        "The induction phase of skin sensitization is associated with the passage of antigen-bearing Langerhans cells (LC) from the epidermis to the draining lymph nodes. Recent investigations have revealed that the induction of LC migration following topical sensitization is dependent on tumour necrosis factor alpha (TNF-alpha), an epidermal cytokine. While in transit to the lymph nodes LC are subject to both phenotypic and functional maturation which, by analogy with in vitro studies, is also effected by epidermal cytokines (granulocyte/macrophage colony-stimulating factor, GM-CSF and interleukin-1, IL-1). It is now apparent that Langerhans cell function, the induction of cutaneous immune responses and effective sensitization are dependent on the availability of such cytokines and that contact allergens are able to provoke their production by keratinocytes and by Langerhans cells themselves. The development of screening strategies for the evaluation of skin sensitization potential as a function of epidermal cytokine production is discussed.";
    String test5 = "Brain structures that control sexual and aggressive behavior in mice are wired differently in females than in males. This the finding of a study led by scientists at NYU School of Medicine and published online Sept. 18 in Nature Neuroscience."+
        "Specifically, researchers found that, while control of aggressive behavior resides in same brain region in female and male mice, certain groups of brain cells in that region are organized differently. Two separate groups of cells were found to control sex and aggression in females, whereas circuits that encourage sex and aggression in males overlapped, say the study authors."+
        "Knowing how aggressive behaviors are regulated is important because they are essential to survival in mice, as well as in humans, which have evolved to compete for food, mates, and territory, researchers say.";
    LSACosineKeywordExtraction kex = new LSACosineKeywordExtraction("");


    List<String> testList = new ArrayList<>();
    testList.add(test3);

    List<String> toCompare = new ArrayList<>();

    toCompare.addAll(kex.readGlossay("data/Glossary_of_biology.txt"));
    toCompare.addAll(kex.readGlossay("data/Glossary_of_chemistry_terms.txt"));
    toCompare.addAll(kex.readGlossay("data/mathematics.txt"));

    //toCompare.add("science");
    System.out.println(kex.extractKeywordsFromTexts(testList, toCompare,5));
    
    KeywordExtractor ke = new InnenExtractor("");
    System.out.println(ke.extractKeywordsFromTexts(testList, 5));
    /*
    StanfordnlpAnalyzer analyzer = new StanfordnlpAnalyzer();
    StanfordLemmatizer lemmStan = new StanfordLemmatizer();
    Lemmatizer lemm = new Lemmatizer();
    long startTime = 0;

    startTime = System.currentTimeMillis();
    System.out.println(kex.createSentencesFromText(test));
    System.out.println(System.currentTimeMillis() - startTime);



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
