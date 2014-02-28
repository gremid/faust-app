YUI.add('text-annotation', function (Y) {

    var translateSegments = function(segment) {
        var offset = segment[0], length = (segment[1] - segment[0]);
        return function(a) {
            return Y.merge(a, { "txt:segment": [ Math.max(0, a["txt:segment"][0] - offset), Math.min(length, a["txt:segment"][1] - offset) ] });
        };
    };

    var Text = function(content, annotations, tree) {
        this._content = (content || "");
        this.length = this._content.length;
        this.annotations = (annotations || []);
        this.tree = tree;
        this.tree && (this.tree.text = this);
    };

    Y.extend(Text, Object, {
        content: function(segment) {
            return (segment ? this._content.substring(Math.max(0, segment[0]), Math.min(segment[1], this.length)) : this._content);
        },
        milestones: function() {
            if (this._milestones) return this._milestones;

            var milestones = [];
            Y.Array.each(this.annotations, function (a) {
                var ms = 0, me = milestones.length, segment = a["txt:segment"], start = segment[0], end = segment[1];
                while (ms < me && milestones[ms] < start) ms++;
                if (ms == me || milestones[ms] != start) {
                    milestones.splice(ms, 0, start);
                    me++;
                }
                me--;
                while (me > ms && milestones[me] > end) me--;
                if (milestones[me] != end) milestones.splice(me + 1, 0, end);
            }, this);

            if (milestones.length == 0 || milestones[0] > 0) milestones.unshift(0);
            if (milestones[milestones.length - 1] < this.length) milestones.push(this.length);

            return (this._milestones = milestones);
        },
        lineBreaks: function() {
            var lb = this._content.indexOf("\n", 0), newLines = [];
            while (lb != -1) {
                newLines.push(lb);
                lb = this._content.indexOf("\n", lb + 1);
            }
            return newLines;
        },
        index: function() {
            if (this._index) return this._index;

            var annotations = [];
            Y.Array.each(this.annotations, function(a) { this.push([a["txt:segment"], a]); }, annotations);
            return (this._index = new Y.Faust.SegmentIndex(annotations));
        }
    });

    var TextSchema = Y.Base.create("text-schema", Y.Plugin.Base, [], {
        initializer: function() {
            this.doBefore("_defDataFn", this._beforeDefDataFn);
        },
        _beforeDefDataFn: function(e) {
            var data = e.data && (e.data.responseText || e.data),
                text = (Y.Lang.isString(data) ? Y.JSON.parse(data) : data),
                payload = e.details[0];

            payload.response = arguments.callee.parse(
                (Y.Lang.isObject(text) && text.text ? text.text : text),
                this.get("treeFilter")
            );

            this.get("host").fire("response", payload);

            return new Y.Do.Halt("TextSchema plugin halted _defDataFn");
        }
    }, {
        NS: "schema",

        ATTRS: {
            treeFilter: {
                value: function(t) { return t["xml:name"]; },
                validator: Y.Lang.isFunction
            }
        },

        parse: function(text, treeFilter) {
            var tree = null,
                offset = 0,
                content = "",
                annotations = {},
                treeNodes = {},
                treeStack = [];

            for (var tc = 0, tl = text.length; tc < tl; tc++) {
                var t = text[tc],
                    parentId = (treeStack.length == 0 ?  null : treeStack[treeStack.length - 1]),
                    parent = (parentId == null ? null : treeNodes[parentId]);

                if (Y.Lang.isString(t)) {
                    var end = offset + t.length;
                    if (parent) parent.append({ "id": ("t" + offset), "txt:content": [ offset, end ] });
                    content += t;
                    offset = end;
                } else if (t.s && t.d) {
                    var data = annotations[t.s] = Y.merge(t.d, { "id": t.s, "txt:segment": [ offset ] });

                    if (treeFilter && treeFilter(t.d)) {
                        var node = (parent ? parent.append(tree.createNode()) : (tree = new Y.Tree()).rootNode);
                        node.data = data;
                        treeNodes[t.s] = node;
                        treeStack.push(t.s);
                    }
                } else if (t.e) {
                    annotations[t.e]["txt:segment"].push(offset);

                    if (parentId && (t.e == parentId)) {
                        treeStack.pop();
                        delete treeNodes[t.e];
                    }
                }
            }

            return new Text(content, Y.Object.values(annotations), tree);
        }
    });

    var Collation = function(data) {
        this.data = data || {};
    };

    Y.extend(Collation, Object, {
        alignments: function() {
            return (this.data.alignments || []);
        },
        transpostions: function() {
            return (this.data.transpositions || []);
        },
        milestones: function(witness) {
            var witnessOffset = (witness * 2),
                alignments = this.alignments(),
                transpositions = this.transpostions(),
                alignmentMilestones = [],
                transpositionMilestones = [];

            for (var oc = 0, last = -1; oc < alignments.length; oc++) {
                var alignment = alignments[oc];
                if (last != alignment[witnessOffset]) alignmentMilestones.push(last = alignment[witnessOffset]);
                if (last != alignment[witnessOffset + 1]) alignmentMilestones.push(last = alignment[witnessOffset + 1]);
            }
            for (var tc = 0, last = -1; tc < transpositions.length; tc++) {
                var transposition = transpositions[tc];
                if (last != transposition[witnessOffset]) transpositionMilestones.push(last = transposition[witnessOffset]);
                if (last != transposition[witnessOffset + 1]) transpositionMilestones.push(last = transposition[witnessOffset + 1]);
            }

            return Y.Faust.SortedArray.dedupe(
                Y.Faust.SortedArray.merge(alignmentMilestones, transpositionMilestones, Y.Faust.compareNumbers),
                Y.Faust.compareNumbers
            );
        }
    });

	Y.mix(Y.namespace("Faust"), {
        translateSegments: translateSegments,
		Text: Text,
        TextSchema: TextSchema,
        Collation: Collation
	});
}, '0.0', {
	requires: ["text-index", "util", "base", "substitute", "array-extras", "io", "json", "plugin", "tree"]
});