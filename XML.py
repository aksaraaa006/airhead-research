import nltk
from nltk.etree.ElementTree import ElementTree

def xmlStripper(fileName):
    test = ElementTree().parse(fileName)
    finalList = []
    element=[]
    tups=[]
    wordIDpair = []
    for ch in test.getchildren():
        for i in range(100):
            for word in ch:
                if word.tag=='head':
                    if( int(word.attrib.values()[0][5:7])==i):
                        if(len(word.attrib.values())==2):
                          wordIDpair = [word.text,word.attrib.values()[1]]
                        else:
                          wordIDpair = [word.text,word.attrib.values()[0]]
                        tups.append(wordIDpair)
            if tups!=[]:                        
                element.append(tups)
            tups=[]
        finalList.append(element)
    return finalList
                

