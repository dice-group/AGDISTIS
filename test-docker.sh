# define correct result
RESULT="[{\"disambiguatedURL\":\"http:\/\/dbpedia.org\/resource\/Leipzig_University\",\"offset\":21,\"namedEntity\":\"University of Leipzig\",\"start\":5},{\"disambiguatedURL\":\"http:\/\/dbpedia.org\/resource\/Barack_Obama\",\"offset\":12,\"namedEntity\":\"Barack Obama\",\"start\":30}]"
echo $RESULT > sed 's/ *$//' expected.txt

# get response
curl -s --data-urlencode "text='The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.'" -d type='agdistis' http://localhost:4445/AGDISTIS > sed 's/ *$//' response.txt

# log response
echo "Got response:"
cat response.txt
echo ""

# check
if cmp -s expected.txt response.txt;
then
  echo "OK, correct response!";
  exit 0;
else
  echo "Error! Response doesn't match!";
  exit 1;
fi
