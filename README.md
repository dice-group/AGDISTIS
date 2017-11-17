# AGDISTIS - Agnostic Named Entity Disambiguation

[![Build Status](https://gitlab.com/aksw/AGDISTIS/badges/master/build.svg)](https://gitlab.com/aksw/AGDISTIS/pipelines)
[![Project Stats](https://www.openhub.net/p/AGDISTIS/widgets/project_thin_badge.gif)](https://www.ohloh.net/p/AGDISTIS)
[![BCH compliance](https://bettercodehub.com/edge/badge/AKSW/AGDISTIS)](https://bettercodehub.com/)


>The new web services are available here:
>```
>en http://akswnc9.informatik.uni-leipzig.de:8113/AGDISTIS
>de http://akswnc9.informatik.uni-leipzig.de:8114/AGDISTIS
>es http://akswnc9.informatik.uni-leipzig.de:8115/AGDISTIS
>fr http://akswnc9.informatik.uni-leipzig.de:8116/AGDISTIS
>it http://akswnc9.informatik.uni-leipzig.de:8117/AGDISTIS
>ja http://akswnc9.informatik.uni-leipzig.de:8118/AGDISTIS
>nl http://akswnc9.informatik.uni-leipzig.de:8119/AGDISTIS
>```

This projects aimes at delivering a framework for disambiguating a priori annotated named entities.

More information about the project can be found <a href="http://aksw.org/projects/AGDISTIS">here</a> and in our <a href="https://github.com/AKSW/AGDISTIS/wiki">Wiki</a>.

Supplementary material can be found in the documents folder.

We hope you will enjoy using AGDISTIS!

### Support and Feedback
If you need help or you have questions do not hesitate to write an email to  <a href="mailto:usbeck@uni-paderborn.de"> Dr. Ricardo Usbeck</a>. Or use the issue tracker in the right sidebar.

### How to cite
```Tex

@InProceedings{Moussallem2017,
  author       = {Diego Moussallem and Ricardo Usbeck and Michael R{\"o}der and Axel-Cyrille {Ngonga Ngomo}},
  title        = {{MAG: A Multilingual, Knowledge-base Agnostic and Deterministic Entity Linking Approach}},
  booktitle    = {K-CAP 2017: Knowledge Capture Conference},
  year         = {2017},
  pages        = {8},
  organization = {ACM},
  url          = {https://svn.aksw.org/papers/2017/KCAP_MAG/sigconf-main.pdf},
}

@incollection{AGDISTIS_ISWC,
  author = {Usbeck, Ricardo and {Ngonga Ngomo}, Axel-Cyrille and Auer, S{\"o}ren and Gerber, Daniel and Both, Andreas},
  booktitle = {13th International Semantic Web Conference},
  title = {AGDISTIS - Graph-Based Disambiguation of Named Entities using Linked Data},
  url = {http://svn.aksw.org/papers/2014/ISWC_AGDISTIS/public.pdf},
  year = 2014
}
```

### Acknowlegements
The first version of this work was supported by the ESF and the Free State of Saxony.
AGDISTIS is now supported by the German Federal Ministry of Education and Research and EuroStars.


### Annotation Tool

The used annotation tool can be downloaded from <a href="https://github.com/RicardoUsbeck/QRTool">here</a>.

### Disclaimer

The deployed webservice does not reflect the optimal parametrization of AGDISTIS as published.

### Bindings
* Python bindings: https://pypi.python.org/pypi/agdistispy/

### Running AGDISTIS

### How to run
```
mvn clean package tomcat:run
```
For more information, go to our <a href="https://github.com/AKSW/AGDISTIS/wiki/3-Running-the-webservice">Wiki</a>.
