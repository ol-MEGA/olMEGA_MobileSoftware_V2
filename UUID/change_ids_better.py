import tkinter as tk
from tkinter import filedialog
import uuid
import os
import re

root = tk.Tk()
root.withdraw()
extension = ".xml"
file_path = filedialog.askopenfilename(title="Please select questionnaire *.xml file", filetypes=(("Quest files", extension),))
# file_path = "I:\olMEGA_MobileSoftware\questionnaires\questionnaire20180425frei - simple.xml"

open(f"{os.path.splitext(file_path)[0]}_UUID{extension}", 'w').close()
f_new = open(f"{os.path.splitext(file_path)[0]}_UUID{extension}", "a")

print(f"File selected: {file_path}")

f_original = open(file_path, "r")
contents = f_original.read()

uuids = {}

idx = 1
quest_split = re.split("<question|</question>", contents)
for quest in quest_split[1::2]:

    print("=====")

    ### question id ###

    quest_id_and_rest = quest.split('id=\"', maxsplit=1)
    quest_id_only_and_rest = quest_id_and_rest[1].split("\"", maxsplit=1)


    if id not in uuids:
        # store id with UUID in dictionary
        uuids[id] = str(uuid.uuid1())
    quest_id_only_and_rest[0] = f'\"{uuids[id]}\"'

    quest_id_and_rest = f' id={"".join(quest_id_only_and_rest)}'
    quest = "".join(quest_id_and_rest)

    ### option id(s) ###

    option_list = re.split("<option|</option>", quest)

    idx_option = 1
    for option in option_list[1::2]:
        option_items = option.split("id=\"", maxsplit=1)
        option_id_and_rest = option_items[1].split("\"", maxsplit=1)
        option_id = option_id_and_rest[0]
        if option_id not in uuids:
            # store id with UUID in dictionary
            uuids[option_id] = str(uuid.uuid1())
        option_id_and_rest[0] = f'\"{uuids[option_id]}\"'
        option_items = f'<option id={"".join(option_id_and_rest)}</option>'
        option_list[idx_option] = option_items
        idx_option += 2
    quest = "".join(option_list)

    ### filters ###

    filter_list = quest.split("filter=\"")
    if (len(filter_list) > 1):
        filter_items_and_rest = filter_list[1].split("\"", maxsplit=1)
        filter_items = filter_items_and_rest[0]
        filter_item_list =  filter_items.split(",")

        for idx_filter, filter_item in enumerate(filter_item_list):

            # in case of line break formatting
            filter_id_minus_n = filter_item.split("\n")
            if len(filter_id_minus_n) > 1:
                # if there is a line break
                filter_id_minus_n[0] = "\n"

                # in case of (multiple) tab formatting
                filter_id_minus_n_t = filter_id_minus_n[1].split("\t")
                if len(filter_id_minus_n_t) > 1:
                    # if there are tabs
                    filter_id = filter_id_minus_n_t[-1].strip()

                    if filter_id.startswith("!"):
                        # negative criterion
                        filter_id_minus_n_t[-1] = "\t" * (len(filter_id_minus_n_t) - 1) + "!" + uuids[filter_id[1:]]
                    else:
                        # positive criterion
                        filter_id_minus_n_t[-1] = "\t" * (len(filter_id_minus_n_t) - 1) + uuids[filter_id]
                else:
                    # no tabs
                    filter_id = filter_id_minus_n_t[0].strip()
                    if filter_id.startswith("!"):
                        filter_id_minus_n_t[-1] = "!" + uuids[filter_id[1:]]
                    else:
                        filter_id_minus_n_t[-1] = uuids[filter_id]

                filter_id_minus_n[1] = "".join(filter_id_minus_n_t)

            else:
                # no line break
                filter_id = filter_id_minus_n[0].strip()
                if filter_id.startswith("!"):
                    # negative criterion
                    filter_id_minus_n[0] = "!" + uuids[filter_id[1:]]
                else:
                    # positive criterion
                    filter_id_minus_n[0] = uuids[filter_id]

            filter_item_list[idx_filter] = "".join(filter_id_minus_n)

        filter_items = " filter=\"" + ",".join(filter_item_list) + "\""
        filter_items_and_rest[0] = filter_items
        filter_list[1] = "".join(filter_items_and_rest)
        quest = "".join(filter_list)








    quest = ["<question", quest, "</question>"]
    quest = "".join(quest)
    quest_split[idx] = quest

    idx += 2



contents = "".join(quest_split)
f_new.write(contents)
f_original.close()
f_new.close()

print(f'Success - {len(uuids)} ids were exchanged.')