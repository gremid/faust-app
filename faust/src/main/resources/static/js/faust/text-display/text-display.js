YUI.add('text-display', function (Y) {

	/**
	 * An area in which text is displayed.
	 * 
	 * All arguments are passed as properties of a configuration object.
	 * 
	 * @param container reference to HTML node acting as output container
	 * @param text instance of Y.Faust.Text which shall be displayed
	 * @param cssPrefix string acting as prefix to all CSS-names generated by this class (optional)
	 * @param renderCallback callback function for handling special rendering (optional).
	 *                       This function is called for every processed annotation and receives the following arguments:
	 *                       	annotation      instance of Y.Faust.Annotation
	 *                       	cssPrefix       the css Prefix configured above
	 *                       	partitionNode   the DOM node of the currently rendered text partition
	 *                       	lineNode        the DOM node of the currently rendered line of text
	 *                       	isFirst         boolean indicating whether this partition constitutes the beginning of the annotation
	 *                       	isLast          boolean indicating whether this partition constitutes the end of the annotation
	 *                       It shall return an array of strings which will be added as CSS classes to the current partition.
	 * 
	 */
	var TextDisplayView = Y.Base.create('textDisplayView', Y.View, [], {
		/**
		 * Outputs an HTML representation of the text to the container
		 */
		render : function(start, end) {
			var text = this.get('text');
			var container = this.get('container');
			var prefix = this.get('cssPrefix');
			var callback = this.get('renderCallback');
			
			start = start || 0;
			end = end || text.contentLength;

			var partitions = text.applyAnnotations(null, start, end);
			
			Y.Array.each(partitions, function(partition, i, partitions){
				var partitionNode = Y.Node.create('<span></span>'); //quite expensive
				container.append(partitionNode);
				var lineNumNode = null;
				
				function isFirst(annotationId)
				{
					return i == 0 || partitions[i-1].annotations[annotationId] === undefined;
				}
				function isLast(annotationId)
				{
					return i+1 == partitions.length || partitions[i+1].annotations[annotationId] === undefined;
				}
				
				Y.Array.each(partition.range.of(text.content).split('\n'), function(line, n) {
					if(n > 0)
						partitionNode.append('<br>');
					
					var lineNode = Y.config.doc.createTextNode(line);
					partitionNode.append(lineNode);
					
					var classes = [];
					for(var id in partition.annotations)
					{
						var annotation = partition.annotations[id];
						var name = prefix + annotation.name.localName;
						
						classes.push(name);
						classes.push(prefix + id);
						
						var first = isFirst(id);
						var last = isLast(id);
						
						if(first)
							classes.push(name + '-first');
						if(last)
							classes.push(name + '-last');
						
						classes = classes.concat(callback(annotation, prefix, partitionNode, lineNode, first, last));
					}
					partitionNode.addClass(classes.join(' '));
				}, this);
			}, this);
		}
    }, {
		ATTRS: {
			container: {}, 
			text: {},
			cssPrefix: {value: ""},
			renderCallback: {value: function(){}},
		}
	});
	
	Y.mix(Y.namespace("Faust"), {
		TextDisplayView: TextDisplayView
	});
}, '0.0', {
	requires: ['base', 'view']
});