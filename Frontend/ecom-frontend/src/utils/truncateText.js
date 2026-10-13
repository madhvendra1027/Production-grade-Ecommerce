const truncateText = (text, charLimit = 90) => {
       if(text?.length > 90){
        return text.slice(0, charLimit) + "...";
       }
       return text;
};

export default truncateText;