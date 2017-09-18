package eu.innovation.engineering.test;

import java.util.ArrayList;
import java.util.List;

import eu.innovation.engineering.LSA.keywordExtractor.LSAKeywordExtractor;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;

public class Tester {



  public static void main(String[] args) throws Exception{
    //init 
    StanfordnlpAnalyzer nlpAnalyzer = new StanfordnlpAnalyzer();


    String test= "World War I (WWI or WW1), also known as the First World War, the Great War, or the War to End All Wars,[5] was a global war originating in Europe that lasted from 28 July 1914 to 11 November 1918. More than 70 million military personnel, including 60 million Europeans, were mobilised in one of the largest wars in history.[6][7] Over nine million combatants and seven million civilians died as a result of the war (including the victims of a number of genocides), a casualty rate exacerbated by the belligerents' technological and industrial sophistication, and the tactical stalemate caused by gruelling trench warfare. It was one of the deadliest conflicts in history, and paved the way for major political changes, including revolutions in many of the nations involved. Unresolved rivalries still extant at the end of the conflict contributed to the start of the Second World War only twenty-one years later.[8]"
        +"The war drew in all the world's economic great powers,[9] assembled in two opposing alliances: the Allies (based on the Triple Entente of the Russian Empire, the French Third Republic, and the United Kingdom of Great Britain and Ireland) versus the Central Powers of Germany and Austria-Hungary. Although Italy was a member of the Triple Alliance alongside Germany and Austria-Hungary, it did not join the Central Powers, as Austria-Hungary had taken the offensive against the terms of the alliance.[10] These alliances were reorganised and expanded as more nations entered the war: Italy, Japan and the United States joined the Allies, while the Ottoman Empire and Bulgaria joined the Central Powers."
        +"The trigger for the war was the assassination of Archduke Franz Ferdinand of Austria, heir to the throne of Austria-Hungary, by Yugoslav nationalist Gavrilo Princip in Sarajevo on 28 June 1914. This set off a diplomatic crisis when Austria-Hungary delivered an ultimatum to the Kingdom of Serbia,[11][12] and entangled international alliances formed over the previous decades were invoked. Within weeks, the major powers were at war and the conflict soon spread around the world."
        +"On 25 July Russia began mobilisation and on 28 July the Austro-Hungarians declared war on Serbia. Germany presented an ultimatum to Russia to demobilise, and when this was refused, declared war on Russia on 1 August. Being outnumbered on the Eastern Front, Russia urged its Triple Entente ally France to open up a second front in the west. Over forty years earlier in 1870, the Franco-Prussian War had ended the Second French Empire and France had ceded the provinces of Alsace-Lorraine to a unified Germany. Bitterness over that defeat and the determination to retake Alsace-Lorraine made the acceptance of Russia's plea for help an easy choice, so France began full mobilisation on 1 August and, on 3 August, Germany declared war on France. The border between France and Germany was heavily fortified on both sides so, according to the Schlieffen Plan, Germany then invaded neutral Belgium and Luxembourg before moving towards France from the north, leading the United Kingdom to declare war on Germany on 4 August due to their violation of Belgian neutrality.[13][14] After the German march on Paris was halted in the Battle of the Marne, what became known as the Western Front settled into a battle of attrition, with a trench line that changed little until 1917. On the Eastern Front, the Russian army led a successful campaign against the Austro-Hungarians, but the Germans stopped its invasion of East Prussia in the battles of Tannenberg and the Masurian Lakes. In November 1914, the Ottoman Empire joined the Central Powers, opening fronts in the Caucasus, Mesopotamia and the Sinai. In 1915, Italy joined the Allies and Bulgaria joined the Central Powers; Romania joined the Allies in 1916, as did the United States in 1917."
        +"The Russian government collapsed in March 1917, and a revolution in November followed by a further military defeat brought the Russians to terms with the Central Powers via the Treaty of Brest Litovsk, which granted the Germans a significant victory. After a stunning German offensive along the Western Front in the spring of 1918, the Allies rallied and drove back the Germans in a series of successful offensives. On 4 November 1918, the Austro-Hungarian empire agreed to an armistice, and Germany, which had its own trouble with revolutionaries, agreed to an armistice on 11 November 1918, ending the war in victory for the Allies."
        +"By the end of the war or soon after, the German Empire, Russian Empire, Austro-Hungarian Empire and the Ottoman Empire ceased to exist. National borders were redrawn, with several independent nations restored or created, and Germany's colonies were parceled out among the victors. During the Paris Peace Conference of 1919, the Big Four (Britain, France, the United States and Italy) imposed their terms in a series of treaties. The League of Nations was formed with the aim of preventing any repetition of such a conflict. This effort failed, and economic depression, renewed nationalism, weakened successor states, and feelings of humiliation (particularly in Germany) eventually contributed to the start of World War II";    LSAKeywordExtractor LSAExtractor = new LSAKeywordExtractor("");
    List<String> list = new ArrayList<String>();
    list.add(test);

    
    //System.out.println(result);


  }



}
