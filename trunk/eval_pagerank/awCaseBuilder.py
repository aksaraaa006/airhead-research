#!/usr/bin/python

from xml.dom.minidom import parse

import pickle
import sys

if __name__ == "__main__":
  xml_file = parse(sys.argv[1])
  case_dict = {}
  for h in xml_file.getElementsByTagName("head"):
    id = h.attributes['id'].value
    file_num, sent_num, leaf_num = id.split(".")
    f_index, s_index, l_index = int(file_num[1:]), int(sent_num[1:]), int(leaf_num[1:])
    if f_index in case_dict:
      s_dict = case_dict[f_index]
    else:
      s_dict = {}
      case_dict[f_index] = s_dict
    if s_index in s_dict:
      l_list = s_dict[s_index]
    else:
      l_list = []
      s_dict[s_index] = l_list
    l_list.append(l_index)
  print case_dict
  pickle.dump(case_dict, open(sys.argv[2], 'w'))
