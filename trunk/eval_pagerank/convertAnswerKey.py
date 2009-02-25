#!/usr/bin/python
#convert the lexical sample answer key into something less rediculous which
# does not rely on verbnet sense id's

from xml.dom.minidom import parse
import sys

def buildWordMapper(in_file):
  map_xml = parse(in_file)
  map_dict = {}
  for sense_map in map_xml.getElementsByTagName('sense'):
    try:
      lemmas = sense_map.attributes['wn'].value.replace(";", " ")
      id = sense_map.attributes['id'].value
      map_dict[id] = lemmas
    except KeyError:
      pass
  return map_dict

if __name__ == "__main__":
  map_dict = buildWordMapper(sys.argv[1])
  answer_key = open(sys.argv[2])
  for line in answer_key:
    pieces = line.split()
    real_lemmas = ""
    for l in pieces[2:]:
      if l in map_dict:
        real_lemmas += map_dict[l]
      else:
        real_lemmas += l + " "
    print pieces[0], pieces[1], real_lemmas

