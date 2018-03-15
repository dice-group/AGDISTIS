#!/bin/bash 

mkdir de
mkdir en
mkdir es
mkdir fr
mkdir it
mkdir ja
mkdir nl

scp -r RicardoUsbeck@akswnc9.informatik.uni-leipzig.de:/home/JonathanEberle/agdistis/de/logs de
scp -r RicardoUsbeck@akswnc9.informatik.uni-leipzig.de:/home/JonathanEberle/agdistis/en/logs en
scp -r RicardoUsbeck@akswnc9.informatik.uni-leipzig.de:/home/JonathanEberle/agdistis/es/logs es
scp -r RicardoUsbeck@akswnc9.informatik.uni-leipzig.de:/home/JonathanEberle/agdistis/fr/logs fr
scp -r RicardoUsbeck@akswnc9.informatik.uni-leipzig.de:/home/JonathanEberle/agdistis/it/logs it
scp -r RicardoUsbeck@akswnc9.informatik.uni-leipzig.de:/home/JonathanEberle/agdistis/ja/logs ja
scp -r RicardoUsbeck@akswnc9.informatik.uni-leipzig.de:/home/JonathanEberle/agdistis/nl/logs nl

#!/bin/bash

for f in de/logs/*.log
do
 grep 'AGDISTIS\t' $f | awk '{print $2}' >> calldate_de.txt
done

wc -l calldate_de.txt


for f in en/logs/*.log
do
 grep 'AGDISTIS\t' $f | awk '{print $2}' >> calldate_en.txt
done

wc -l calldate_en.txt


for f in es/logs/*.log
do
 grep 'AGDISTIS\t' $f | awk '{print $2}' >> calldate_es.txt
done

wc -l calldate_es.txt


for f in fr/logs/*.log
do
 grep 'AGDISTIS\t' $f | awk '{print $2}' >> calldate_fr.txt
done

wc -l calldate_fr.txt

for f in it/logs/*.log
do
 grep 'AGDISTIS\t' $f | awk '{print $2}' >> calldate_it.txt
done

wc -l calldate_it.txt

for f in ja/logs/*.log
do
 grep 'AGDISTIS\t' $f | awk '{print $2}' >> calldate_ja.txt
done

wc -l calldate_ja.txt

for f in 'nl/logs/*.log'
do
 grep 'AGDISTIS\t' $f | awk '{print $2}' >> calldate_nl.txt
done

wc -l calldate_nl.txt
