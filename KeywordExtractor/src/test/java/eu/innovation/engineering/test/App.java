package eu.innovation.engineering.test;

import java.util.ArrayList;
import java.util.List;

import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.maui.main.MauiWrapper;

public class App {
  
  
  public static void main(String[] args) throws Exception{
    MauiWrapper mauiwrapper = new MauiWrapper("", "none", "newInnenModel");
    
    
    String test = "Spiritual but not religious: a phenomenological study of spirituality in the everyday lives of younger women in contemporary Australia"+
"In current discussions about contemporary forms of spirituality, consideration is given to the question, what is spirituality and to exploring the range of associated beliefs and practices. Common to most discussions is the acknowledgement that the term spirituality is ambiguous and does not represent any one finite quality or thing, but rather, is a wide and somewhat identifiable set of characteristics. Some commentators suggest that contemporary spirituality, characterised by its separation from institutional forms of religion, and represented by the hallmark expression I am spiritual, but not religious, is an increasing phenomenon in Australian society. In view of this, there are several debates about the merits of a spirituality without explicit links to religion (in particular Christian traditions) and whether a personal spirituality can hold any real depth or purpose, or whether it just perpetuates a superficial, narcissistic focus of the self. This kind of critique pays little attention as to how spirituality, and the associated beliefs and practices, are developed and applied in an everyday sense, and how this impacts on the lives of those who subscribe to their own sense of spirituality. "+
"In this thesis, I shift the focus from analysing the merits of a personalised spirituality to exploring in depth some of the lay understandings and purposes underlying contemporary forms of spiritual practice. The primary concern of my thesis is to describe this phenomena of spiritual life as experienced by eleven younger Australian women aged 18-38 years inclusive, who considered themselves 'spiritual' women, yet do not necessarily identify with a particular religious denomination. At its core, and as a phenomenological study, the thesis undertakes a theoretical exploration of consciousness and the apprehension and formation of belief, meaning, and identity. Held central, and alongside the phenomenological methodology, is the feminist notion that every woman is the centre of her own experience, that any interpretations and understandings of women's spirituality, must start with the personal. The empirical stages of research therefore focus on an exploration of the women's personal understandings, experiences, interpretations and translations of spirituality to uncover the location and application of spirituality in everyday life. "
+"A primary factor explored throughout the thesis is the intersection between emotional experiences, meaning and purpose, and notions of spirituality. It is my assertion that grief, crisis and trauma, and the more general emotional experiences arising from everyday life, can be a driving force to embark on an exploration of the spiritual; inform personal constructions of spirituality; and provide a basis for the articulation of that spirituality, with a central purpose of alleviating emotional pain. Thus, my main thesis contention is this 'new' form of spirituality, as experienced and practiced outside of religious institutions, was expressed by the women in this research as a conscious and pragmatic resource applied, and developed in relation to, the various events and experiences of everyday life, and in relation to the ongoing process of developing and locating a sense of self and identity.";
   
    List<String> toTest = new ArrayList<>();
    toTest.add(test);
    
    System.out.println(mauiwrapper.extractKeywordsFromText(toTest));
    
    
    InnenExtractor innwrapper = new InnenExtractor();
    System.out.println(innwrapper.extractKeywordsFromText(toTest));
    
    
  }

}
