# define correct result
RESULT='[{"disambiguatedURL":"http:\/\/dbpedia.org\/resource\/Angela_Merkel","offset":13,"namedEntity":"Angela Merkel","start":1},{"disambiguatedURL":"http:\/\/dbpedia.org\/resource\/Barack_Obama","offset":12,"namedEntity":"Barack Obama","start":19},{"disambiguatedURL":"http:\/\/dbpedia.org\/resource\/Berlin","offset":6,"namedEntity":"Berlin","start":39}]'
echo $RESULT > expected.txt

# get response
curl -s --data-urlencode "text='<entity>Angela Merkel</entity> and <entity>Barack Obama</entity> are in <entity>Berlin</entity>.'" -d type='agdistis' http://localhost:8080/AGDISTIS > response.txt
echo "" >> response.txt

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
